package json.reads

import play.api.libs.functional.syntax._
import play.api.libs.json._
import lib.model.Category

import ixias.util.json.JsonEnvReads

case class JsValueCategoryDelete(
  id: Category.Id
)
object JsValueCategoryDelete extends JsonEnvReads {
  implicit val todoIdReads: Reads[Category.Id] = idAsNumberReads
  implicit val reads: Reads[JsValueCategoryDelete] = Json.reads
}