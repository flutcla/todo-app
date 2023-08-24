package json.reads

import play.api.libs.functional.syntax._
import play.api.libs.json._
import lib.model.User

import ixias.util.json.JsonEnvReads

case class JsValueUserLogin(
  email:    String,
  password: String
)
object JsValueUserLogin extends JsonEnvReads {
  implicit val reads: Reads[JsValueUserLogin] = Json.reads
}
