/**
 *
 * to do sample project
 *
 */

package model

import java.time.LocalDateTime

case class ViewValueTodo(
  title:  String,
  cssSrc: Seq[String],
  jsSrc:  Seq[String],
) extends ViewValueCommon

case class Todo(
  id:          Long,
  category_id: Long,
  title:       String,
  body:        String,
  state:       Int,
  updated_at:  LocalDateTime,
  created_at:  LocalDateTime
)