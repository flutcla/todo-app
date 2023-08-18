package json.reads

import java.awt.Color

import play.api.libs.functional.syntax._
import play.api.libs.json._
import lib.model.Category

import ixias.util.json.JsonEnvReads

case class JsValueCategory(
  id:    Category.Id,
  name:  String,
  slug:  String,
  color: Color
)
object JsValueCategory extends JsonEnvReads {
  implicit val categoryIdReads: Reads[Category.Id] = idAsNumberReads
  implicit val colorReads: Reads[Color] = Reads.of[String].map(colorCode =>
    new Color(Integer.parseInt(colorCode, 16), true)
  )
  implicit val reads: Reads[JsValueCategory] = Json.reads
}