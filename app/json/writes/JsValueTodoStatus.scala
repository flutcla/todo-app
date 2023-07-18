package json.writes

import play.api.libs.json._
import lib.model.Todo

case class JsValueTodoStatus(
  code: Short,
  name: String
)
object JsValueTodoStatus {
  implicit val writes: Writes[JsValueTodoStatus] = Json.writes[JsValueTodoStatus]
  def apply(state: Todo.Status): JsValueTodoStatus = JsValueTodoStatus(
    code = state.code,
    name = state.name
  )
}