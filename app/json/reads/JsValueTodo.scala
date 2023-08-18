package json.reads

import play.api.libs.functional.syntax._
import play.api.libs.json._
import lib.model.{Todo, Category}

import ixias.util.json.JsonEnvReads

case class JsValueTodo(
  id:          Todo.Id,
  categoryId:  Category.Id,
  title:       String,
  body:        String,
  state:       Todo.Status
)
object JsValueTodo extends JsonEnvReads {
  implicit val todoIdReads: Reads[Todo.Id] = idAsNumberReads
  implicit val categoryIdReads: Reads[Category.Id] = idAsNumberReads
  implicit val stateReads: Reads[Todo.Status] = enumReads(Todo.Status)
  implicit val reads: Reads[JsValueTodo] = Json.reads
}