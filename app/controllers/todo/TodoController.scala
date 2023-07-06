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

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

import model.{ViewValueTodo, ViewValueTodoAdd}
import lib.model.Todo
import lib.model.Category
import lib.persistence.default

case class TodoFormData(
  categoryId : Category.Id,
  title :      String,
  body :       String
)

@Singleton
class TodoController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with I18nSupport {
  def list() = Action.async { implicit request: Request[AnyContent] => {
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
        ???  // TODO: Category が見つからなかった際の処理を考える
      } else {
        Ok(views.html.todo.list(ViewValueTodo(
          title  = "Todo 一覧",
          cssSrc = Seq("main.css"),
          jsSrc = Seq("main.js"),
          todoCategorySeq = todoCategoryOptSeq.flatten)))
      }
    }
  }}

  // 登録用
  implicit val categoryIdFormatter = new Formatter[Category.Id] {
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Category.Id] =
      Formats.intFormat.bind(key, data).right.map(Category.Id(_))
    def unbind(key: String, value: Category.Id): Map[String, String] =
      Map(key -> value.toString)
  }
  val form: Form[TodoFormData] = Form(
    mapping(
      "categoryId" -> of[Category.Id],
      "title"      -> nonEmptyText(maxLength = 140),
      "body"       -> nonEmptyText()
    )(TodoFormData.apply)(TodoFormData.unapply(_))
  )

  def add() = Action.async { implicit request: Request[AnyContent] => {
    for {
      categories <- default.CategoryRepository.getAll()
    } yield (
      Ok(views.html.todo.store(ViewValueTodoAdd(
        title      = "Todo 追加",
        cssSrc     = Seq("main.css"),
        jsSrc      = Seq("main.js"),
        form       = form,
        categories = categories
      )))
    )
  }}

  def store() = Action.async { implicit request: Request[AnyContent] => {
    form.bindFromRequest().fold(
      (formWithErrors: Form[TodoFormData]) => {
        for {
          categories <- default.CategoryRepository.getAll()
        } yield (
          BadRequest(views.html.todo.store(ViewValueTodoAdd(
            title      = "Todo 追加",
            cssSrc     = Seq("main.css"),
            jsSrc      = Seq("main.js"),
            form       = form,
            categories = categories
          )))
        )
      },
      (todoFormData: TodoFormData) => {
        for {
          result <- default.TodoRepository.add(Todo.apply(
            categoryId = Category.Id(todoFormData.categoryId),
            title      = todoFormData.title,
            body       = todoFormData.body,
            state      = Todo.Status.TODO
          ))
        } yield (
          Redirect(routes.TodoController.list())
        )
      }
    )
  }}
}
