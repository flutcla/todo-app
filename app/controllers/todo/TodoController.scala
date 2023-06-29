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
  categoryId : Int,
  title :      String,
  body :       String
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
      "categoryId" -> number,
      "title"      -> nonEmptyText(maxLength = 140),
      "body"       -> nonEmptyText()
    )(TodoFormData.apply)(TodoFormData.unapply(_))
  )

  def add() = Action { implicit request: Request[AnyContent] => {
    val vv = ViewValueTodo(
      title  = "Todo 追加",
      cssSrc = Seq("main.css"),
      jsSrc  = Seq("main.js")
    )
    Ok(views.html.todo.store(vv, form))
  }}

  def store() = Action.async { implicit request: Request[AnyContent] => {
    val vv = ViewValueTodo(
      title  = "Todo 追加",
      cssSrc = Seq("main.css"),
      jsSrc  = Seq("main.js")
    )
    form.bindFromRequest().fold(
      (formWithErrors: Form[TodoFormData]) => {
        Future{BadRequest(views.html.todo.store(vv, formWithErrors))}
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
}
