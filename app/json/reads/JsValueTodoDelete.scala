package json.reads

import play.api.libs.functional.syntax._
import play.api.libs.json._
import lib.model.Todo

import ixias.util.json.JsonEnvReads

case class JsValueTodoDelete(
  id: Todo.Id
)
object JsValueTodoDelete extends JsonEnvReads {
  implicit val todoIdReads: Reads[Todo.Id] = idAsNumberReads
  implicit val reads: Reads[JsValueTodoDelete] = Json.reads
}