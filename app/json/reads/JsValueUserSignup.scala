package json.reads

import play.api.libs.functional.syntax._
import play.api.libs.json._
import lib.model.User

import ixias.util.json.JsonEnvReads

case class JsValueUserSignup(
  name:     String,
  email:    String,
  password: String
)
object JsValueUserSignup extends JsonEnvReads {
  implicit val reads: Reads[JsValueUserSignup] = Json.reads
}
