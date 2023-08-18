package json.writes

import play.api.libs.json._
import lib.model.{Todo, Category}

case class JsValueTodo(
  id:         Todo.Id,
  categoryId: Category.Id,
  title:      String,
  body:       String,
  state:      Todo.Status
)

object JsValueTodo {
  implicit val todoWrites = Writes[JsValueTodo] { todo =>
    Json.obj(
      "id"         -> todo.id.toLong,
      "categoryId" -> todo.categoryId.toLong,
      "title"      -> todo.title,
      "body"       -> todo.body,
      "state"      -> JsValueTodoStatus(todo.state),
    )
  }

  def apply(todo: Todo#EmbeddedId): JsValueTodo =
    JsValueTodo(
      id         = todo.id,
      categoryId = todo.v.categoryId,
      title      = todo.v.title,
      body       = todo.v.body,
      state      = todo.v.state
    )

}
