package controllers.category

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import play.api.mvc.BaseController
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.i18n.I18nSupport

import scala.concurrent.ExecutionContext.Implicits.global

import model.{ViewValueCategoryList}
import lib.persistence.default

@Singleton
class CategoryController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with I18nSupport {
  def list() = Action.async {implicit request: Request[AnyContent] => {
    val categoryFuture = default.CategoryRepository.getAll()
    for {
      categories <- categoryFuture
    } yield Ok(views.html.category.list(ViewValueCategoryList(
      title  = "Category 一覧",
      cssSrc = Seq("main.css"),
      jsSrc = Seq("main.js"),
      categories = categories
    )))
  }}
}