package json.reads

import java.awt.Color

import play.api.libs.functional.syntax._
import play.api.libs.json._
import lib.model.Category

import ixias.util.json.JsonEnvReads

case class JsValueCategoryStore(
  name:  String,
  slug:  String,
  color: Color
)
object JsValueCategoryStore extends JsonEnvReads {
  implicit val colorReads: Reads[Color] = Reads.of[String].map(colorCode =>
    new Color(Integer.parseInt(colorCode.substring(1, 7), 16), true)
  )
  implicit val reads: Reads[JsValueCategoryStore] = Json.reads
}