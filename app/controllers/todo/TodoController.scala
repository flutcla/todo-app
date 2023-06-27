package controllers.todo

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import play.api.mvc.BaseController
import play.api.mvc.Request
import play.api.mvc.AnyContent
import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

import model.ViewValueTodo
import lib.model.Todo
import lib.persistence.onMySQL

@Singleton
class TodoController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def list() = Action.async { implicit request: Request[AnyContent] => {
    val vv = ViewValueTodo(
      title  = "Todo 一覧",
      cssSrc = Seq("main.css"),
      jsSrc  = Seq("main.js")
    )

    for {
      results <- onMySQL.TodoRepository.getAll()
    } yield (
      Ok(views.html.todo.list(vv, results))
    )
  }}
}
