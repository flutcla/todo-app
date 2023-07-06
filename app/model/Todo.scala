/**
 *
 * to do sample project
 *
 */

package model

import play.api.data.Form
import lib.model.{Todo, Category}
import controllers.todo.TodoFormData

case class ViewValueTodo(
  title:           String,
  cssSrc:          Seq[String],
  jsSrc:           Seq[String],
  todoCategorySeq: Seq[Tuple2[Todo#EmbeddedId, Category#EmbeddedId]]
) extends ViewValueCommon

case class ViewValueTodoAdd(
  title:           String,
  cssSrc:          Seq[String],
  jsSrc:           Seq[String],
  form:            Form[TodoFormData],
  categories:      Seq[Category#EmbeddedId]
) extends ViewValueCommon {
  lazy val categoriesIdNameTuple = categories.map{cat => (cat.id.toString, cat.v.name)}
}
