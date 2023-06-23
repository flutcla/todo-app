package controllers.todo

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import play.api.mvc.BaseController
import play.api.mvc.Request
import play.api.mvc.AnyContent
import java.time.LocalDateTime
import model.ViewValueTodo
import model.Todo

@Singleton
class TodoController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def list() = Action { implicit request: Request[AnyContent] => {
    val vv = ViewValueTodo(
      title  = "Todo 一覧",
      cssSrc = Seq("main.css"),
      jsSrc  = Seq("main.js")
    )

    val todos: Seq[Todo] = Seq(
      Todo(
        1L,
        1L,
        "サンプルのTodo1",
        "これはサンプルのTodoです",
        1,
        LocalDateTime.of(2000, 1, 1, 0, 0),
        LocalDateTime.of(2000, 1, 1, 0, 0)
      ),
      Todo(
        2L,
        2L,
        "サンプルのTodo2",
        "これはサンプルのTodo その2です",
        1,
        LocalDateTime.of(2002, 1, 1, 0, 0),
        LocalDateTime.of(2002, 1, 1, 0, 0)
      )
    )

    Ok(views.html.todo.list(vv, todos))
  }}
}
