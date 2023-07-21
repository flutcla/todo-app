package json.reads

import play.api.libs.functional.syntax._
import play.api.libs.json._
import lib.model.{Todo, Category}

import ixias.util.json.JsonEnvReads

case class JsValueTodoStore(
    categoryId:  Category.Id,
    title:       String,
    body:        String
)
object JsValueTodoStore extends JsonEnvReads {
  implicit val categoryIdReads: Reads[Category.Id] = idAsNumberReads
  implicit val reads: Reads[JsValueTodoStore] = Json.reads
}