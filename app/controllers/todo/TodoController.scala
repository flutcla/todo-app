package controllers.todo

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import play.api.mvc.BaseController
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.data.FormError
import play.api.data.format.{ Formats, Formatter }

import cats.data.OptionT
import cats.implicits._

import java.time.LocalDateTime
import scala.concurrent._
import scala.concurrent.duration.Duration

import model.{ViewValueTodo, ViewValueTodoAdd, ViewValueTodoEdit}
import lib.model.Todo
import lib.model.Category
import lib.persistence.default

import json.writes._
import json.reads._
import play.api.libs.json.Json

import mvc.auth.UserAuthProfile
import ixias.play.api.auth.mvc.AuthExtensionMethods

case class TodoFormData(
  categoryId : Category.Id,
  title :      String,
  body :       String
)

case class TodoEditFormData(
  categoryId : Category.Id,
  title :      String,
  body :       String,
  state:       Todo.Status
)

@Singleton
class TodoController @Inject()(
  val controllerComponents: ControllerComponents,
  val authProfile:          UserAuthProfile,
) (implicit ec: ExecutionContext) extends AuthExtensionMethods
  with BaseController
  with I18nSupport
{
  def list() = Authenticated(authProfile).async { implicit request: Request[AnyContent] => {
    val todoFuture = default.TodoRepository.getAll()
    val categoryFuture = default.CategoryRepository.getAll()
    for {
      todos <- todoFuture
      categories <- categoryFuture
    } yield {
      val todoCategoryOptSeq: Seq[Option[Tuple2[Todo#EmbeddedId, Category#EmbeddedId]]] = todos.map(res =>
          categories.collectFirst{
            case cat if cat.id == res.v.categoryId => (res, cat)
          }
        )
      if (todoCategoryOptSeq.contains(None)) {
        NotFound(views.html.error.page404())
      } else {
        val jsValue = todoCategoryOptSeq.flatten.map {
          case (todo, category) => (JsValueTodoWithCategory((todo, category)))
        }
        Ok(Json.toJson(jsValue))
      }
    }
  }}

  // 1件取得
  def single(id: Long) = Authenticated(authProfile).async { implicit request: Request[AnyContent] => {
    val ot: OptionT[Future, play.api.mvc.Result] = for {
      todo <- OptionT(default.TodoRepository.get(Todo.Id(id)))
      category <- OptionT(default.CategoryRepository.get(todo.v.categoryId))
    } yield (
      Ok(Json.toJson(JsValueTodoWithCategory((todo, category))))
    )
    ot.getOrElse(
      NotFound(Json.toJson("message" -> s"ID ${id} does not exist."))
    )
  }}

  // 登録用
  def store() = Authenticated(authProfile)(parse.json).async { implicit request => {
    request.body
      .validate[JsValueTodoStore]
      .fold(
        errors => Future.successful(BadRequest(Json.toJson("message" -> "The format is wrong."))),
        todoStoreData =>
        for {
          result <- default.TodoRepository.add(Todo.apply(
            categoryId = Category.Id(todoStoreData.categoryId),
            title      = todoStoreData.title,
            body       = todoStoreData.body,
            state      = Todo.Status.TODO
          ))
        } yield (
          Redirect(routes.TodoController.list())
        )
      )
    }
  }

  /**
  * 対象のデータを削除する
  */
  def delete() = Authenticated(authProfile)(parse.json).async { implicit request => {
    request.body
      .validate[JsValueTodoDelete]
      .fold(
        errors => Future.successful(BadRequest(Json.toJson("message" -> "The format is wrong."))),
        todoDeleteData =>{
          for {
            res <- default.TodoRepository.remove(todoDeleteData.id)
          } yield (
            res match {
              case Some(x) => {
                Ok(Json.toJson("message" -> s"Successfully deleted ${todoDeleteData.id.toLong}."))
              }
              case None => NotFound(Json.toJson("message" -> s"ID ${todoDeleteData.id.toLong} does not exist."))
            }
          )
        }
      )
    }
  }

  /**
   * 編集
   */
  implicit val categoryIdFormatter = new Formatter[Category.Id] {
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Category.Id] =
      Formats.intFormat.bind(key, data).right.map(Category.Id(_))
    def unbind(key: String, value: Category.Id): Map[String, String] =
      Map(key -> value.toString)
  }
  val editForm = Form(
    mapping(
      "categoryId" -> of[Category.Id],
      "title"      -> nonEmptyText(maxLength = 140),
      "body"       -> nonEmptyText(),
      "state"      -> shortNumber.transform[Todo.Status](Todo.Status(_), _.code)
    )(TodoEditFormData.apply)(TodoEditFormData.unapply(_))
  )

  def edit(id: Long) = Authenticated(authProfile).async { implicit request: Request[AnyContent] => {
    for {
      res <- default.TodoRepository.get(Todo.Id(id))
      categories <- default.CategoryRepository.getAll()
    } yield (
      res match {
        case Some(todo) => Ok(views.html.todo.edit(
          ViewValueTodoEdit(
            title = "Todo 追加",
            cssSrc = Seq("main.css"),
            jsSrc = Seq("main.js"),
            id = Todo.Id(id),
            form = editForm.fill(TodoEditFormData(
              todo.v.categoryId,
              todo.v.title,
              todo.v.body,
              todo.v.state
            )),
            categories = categories,
            status = Todo.Status.values
          )
        ))
        case None => NotFound(views.html.error.page404())
      }
    )
  }}

  def update(id: Long) = Authenticated(authProfile)(parse.json).async { implicit request => {
    request.body
      .validate[json.reads.JsValueTodo]
      .fold(
        errors => Future.successful(BadRequest(Json.toJson("message" -> "The format is wrong."))),
        todoData => {
          val ot: OptionT[Future, play.api.mvc.Result] = for {
            old <- OptionT(default.TodoRepository.get(Todo.Id(id)))
            updateData = old.map(_.copy(
              categoryId = todoData.categoryId,
              title      = todoData.title,
              body       = todoData.body,
              state      = todoData.state
            ))
            _ <- OptionT(default.TodoRepository.update(updateData))
          } yield Redirect(routes.TodoController.list())
          ot.getOrElse(NotFound(Json.toJson("message" -> "")))
        }
      )
  }}
}
