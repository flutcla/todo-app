package controllers.todo

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import play.api.mvc.BaseController
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import model.ViewValueTodo
import lib.model.Todo
import lib.persistence.onMySQL

case class TodoFormData(
  categoryId : Long,
  title :      String,
  body :       String
)

case class TodoEditFormData(
  categoryId : Long,
  title :      String,
  body :       String,
  state:       Short
)

@Singleton
class TodoController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with I18nSupport {
  def list() = Action.async { implicit request: Request[AnyContent] => {
    val vv = ViewValueTodo(
      title  = "Todo 一覧",
      cssSrc = Seq("main.css"),
      jsSrc  = Seq("main.js")
    )

    for {
      results <- onMySQL.TodoRepository.getAll()
      categories <- onMySQL.CategoryRepository.getAll()
    } yield (
      Ok(views.html.todo.list(
          vv,
          results.map(res =>
          (
            res,
            categories
              .filter(_.id == res.v.categoryId)
              .headOption
              .get
          )
        )
      ))
    )
  }}

  // 登録用
  val form = Form(
    mapping(
      "categoryId" -> longNumber,
      "title"      -> nonEmptyText(maxLength = 140),
      "body"       -> nonEmptyText()
    )(TodoFormData.apply)(TodoFormData.unapply(_))
  )

  def add() = Action.async { implicit request: Request[AnyContent] => {
    val vv = ViewValueTodo(
      title  = "Todo 追加",
      cssSrc = Seq("main.css"),
      jsSrc  = Seq("main.js")
    )
    for {
      categories <- onMySQL.CategoryRepository.getAll()
    } yield (
      Ok(views.html.todo.store(
        vv,
        form,
        categories.map(cat => (
          cat.id.toString, cat.v.name
        ))
      ))
    )
  }}

  def store() = Action.async { implicit request: Request[AnyContent] => {
    val vv = ViewValueTodo(
      title  = "Todo 追加",
      cssSrc = Seq("main.css"),
      jsSrc  = Seq("main.js")
    )
    form.bindFromRequest().fold(
      (formWithErrors: Form[TodoFormData]) => {
        for {
          categories <- onMySQL.CategoryRepository.getAll()
        } yield (
          BadRequest(views.html.todo.store(
            vv,
            formWithErrors,
            categories.map(cat => (
              cat.id.toString, cat.v.name
            ))
          ))
        )
      },
      (todoFormData: TodoFormData) => {
        for {
          result <- onMySQL.TodoRepository.add(Todo.apply(
            categoryId = todoFormData.categoryId,
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

  /**
  * 対象のデータを削除する
  */
  def delete() = Action.async { implicit request: Request[AnyContent] => {
    request.body.asFormUrlEncoded.get("id").headOption match {
      case Some(id) =>
        for {
          res <- onMySQL.TodoRepository.get(Todo.Id(id.toLong))
          _ <- onMySQL.TodoRepository.remove(Todo.Id(id.toLong))
        } yield (
          res match {
            case Some(x) => {
              Redirect(routes.TodoController.list())
            }
            case None => NotFound(views.html.error.page404())
          }
        )
      case None => Future{
        NotFound(views.html.error.page404())
      }
    }
  }}

  /**
   * 編集画面
   */
  val editForm = Form(
    mapping(
      "categoryId" -> longNumber,
      "title"      -> nonEmptyText(maxLength = 140),
      "body"       -> nonEmptyText(),
      "state"      -> shortNumber
    )(TodoEditFormData.apply)(TodoEditFormData.unapply(_))
  )

  def edit(id: Long) = Action.async { implicit request: Request[AnyContent] => {
    for {
      res <- onMySQL.TodoRepository.get(Todo.Id(id))
      categories <- onMySQL.CategoryRepository.getAll()
    } yield (
      res match {
        case Some(todo) => Ok(views.html.todo.edit(
          ViewValueTodo(title  = "Todo 追加", cssSrc = Seq("main.css"), jsSrc  = Seq("main.js")),
          id,
          editForm.fill(TodoEditFormData(
            todo.v.categoryId,
            todo.v.title,
            todo.v.body,
            todo.v.state.code
          )),
          categories.map(cat => (cat.id.toString, cat.v.name)),
          Todo.Status.values.map(s => (s.code.toString, s.name))
        ))
        case None => NotFound(views.html.error.page404())
      }
    )
  }}

  def update(id: Long) = Action.async { implicit request: Request[AnyContent] => {
    editForm.bindFromRequest().fold(
      (formWithErrors: Form[TodoEditFormData]) => {
        for {
          categories <- onMySQL.CategoryRepository.getAll()
        } yield (
          BadRequest(views.html.todo.edit(
            ViewValueTodo(title  = "Todo 追加", cssSrc = Seq("main.css"), jsSrc  = Seq("main.js")),
            id,
            formWithErrors,
            categories.map(cat => (cat.id.toString, cat.v.name)),
            Todo.Status.values.map(s => (s.code.toString, s.name))
          ))
        )
      },
      (editFormData: TodoEditFormData) => {
        for {
          old <- onMySQL.TodoRepository.get(Todo.Id(id))
          res <- onMySQL.TodoRepository.update(old.get.map(_.copy(
            categoryId = editFormData.categoryId,
            title      = editFormData.title,
            body       = editFormData.body,
            state      = Todo.Status(editFormData.state),
            updatedAt  = java.time.LocalDateTime.now()
          )))
        } yield (
          res match {
            case Some(_) => Redirect(routes.TodoController.list())
            case None => NotFound(views.html.error.page404())
          }
        )
      }
    )
  }}
}
