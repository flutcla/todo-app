package controllers.todo

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import play.api.mvc.BaseController
import play.api.mvc.Request
import play.api.mvc.AnyContent
import java.time.LocalDateTime
import model.ViewValueTodo
import lib.model.Todo

@Singleton
class TodoController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def list() = Action { implicit request: Request[AnyContent] => {
    val vv = ViewValueTodo(
      title  = "Todo 一覧",
      cssSrc = Seq("main.css"),
      jsSrc  = Seq("main.js")
    )

    val todos: Seq[Todo#EmbeddedId] = Seq(
      new Todo(
        id = Some(Todo.Id(1L)),
        category_id = 1L,
        title = "サンプルのTodo1",
        body = "これはサンプルのTodoです",
        Todo.Status(1),
        LocalDateTime.of(2000, 1, 1, 0, 0),
        LocalDateTime.of(2000, 1, 1, 0, 0)
      ).toEmbeddedId,
      new Todo(
        id = Some(Todo.Id(2L)),
        category_id = 1L,
        title = "サンプルのTodo2",
        body = "これはサンプルのTodo その2です",
        Todo.Status(2),
        LocalDateTime.of(2002, 1, 1, 0, 0),
        LocalDateTime.of(2002, 1, 1, 0, 0)
      ).toEmbeddedId,
    )

    Ok(views.html.todo.list(vv, todos))
  }}
}
