/**
 *
 * to do sample project
 *
 */

package model

case class ViewValueTodo(
  title:  String,
  cssSrc: Seq[String],
  jsSrc:  Seq[String],
) extends ViewValueCommon
