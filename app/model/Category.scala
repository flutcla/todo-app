package model

import lib.model.Category

case class ViewValueCategoryList(
  title:           String,
  cssSrc:          Seq[String],
  jsSrc:           Seq[String],
  categories:      Seq[Category#EmbeddedId]
) extends ViewValueCommon