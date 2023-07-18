package json.writes

import java.awt.Color

import play.api.libs.json._
import lib.model.Category

case class JsValueCategory(
  id:    Category.Id,
  name:  String,
  slug:  String,
  color: Color
)

object JsValueCategory {
  implicit val categoryWrites = Writes[JsValueCategory] { category =>
    Json.obj(
      "id"    -> category.id.toLong,
      "name"  -> category.name,
      "slug"  -> category.slug,
      "color" -> f"#${category.color.getRGB}%06x"
    )
  }

  def apply(category: Category#EmbeddedId): JsValueCategory =
    JsValueCategory(
      id    = category.id,
      name  = category.v.name,
      slug  = category.v.slug,
      color = category.v.color
    )
}