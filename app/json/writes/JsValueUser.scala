package json.writes

import play.api.libs.functional.syntax._
import play.api.libs.json._
import lib.model.User

import ixias.util.json.JsonEnvWrites

case class JsValueUser(
  id:       Long,
  name:     String,
  email:    String,
)
object JsValueUser extends JsonEnvWrites {
  implicit val writes: Writes[JsValueUser] = Json.writes
}