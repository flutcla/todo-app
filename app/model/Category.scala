package model

import play.api.data.Form
import lib.model.Category
import controllers.category.{CategoryFormData}

case class ViewValueCategoryList(
  title:           String,
  cssSrc:          Seq[String],
  jsSrc:           Seq[String],
  categories:      Seq[Category#EmbeddedId]
) extends ViewValueCommon

case class ViewValueCategoryAdd(
  title:           String,
  cssSrc:          Seq[String],
  jsSrc:           Seq[String],
  form:            Form[CategoryFormData]
) extends ViewValueCommon

case class ViewValueCategoryEdit(
  title:           String,
  cssSrc:          Seq[String],
  jsSrc:           Seq[String],
  id:              Category.Id,
  form:            Form[CategoryFormData]
) extends ViewValueCommon