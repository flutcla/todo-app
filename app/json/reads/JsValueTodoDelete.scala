package json.reads

import play.api.libs.functional.syntax._
import play.api.libs.json._
import lib.model.Todo

case class JsValueTodoDelete(
  id: Todo.Id
)
object JsValueTodoDelete {
  implicit val todoIdReads: Reads[Todo.Id] = Reads.of[Long].map(Todo.Id.apply)
  implicit val reads: Reads[JsValueTodoDelete] = (
    (JsPath \ "id").read[Todo.Id](todoIdReads)
  ).map(JsValueTodoDelete.apply)
}