/**
 *
 * to do sample project
 *
 */

package model

import lib.model.{Todo, Category}

case class ViewValueTodo(
  title:           String,
  cssSrc:          Seq[String],
  jsSrc:           Seq[String],
  todoCategorySeq: Seq[Tuple2[Todo#EmbeddedId, Category#EmbeddedId]]
) extends ViewValueCommon
