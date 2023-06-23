/**
 *
 * to do sample project
 *
 */

package model

import java.time.LocalDate

case class ViewValueTodo(
  title:  String,
  cssSrc: Seq[String],
  jsSrc:  Seq[String],
) extends ViewValueCommon
