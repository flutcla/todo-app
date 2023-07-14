package controllers.category

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import play.api.mvc.BaseController
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.i18n.I18nSupport
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.FormError
import play.api.data.format.{ Formats, Formatter }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}
import java.awt.Color

import model.{ViewValueCategoryList, ViewValueCategoryAdd}
import lib.persistence.default
import lib.model.Category

case class CategoryFormData(
  name:  String,
  slug:  String,
  color: Color
)

@Singleton
class CategoryController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with I18nSupport {
  def list() = Action.async {implicit request: Request[AnyContent] => {
    for {
      categories <- default.CategoryRepository.getAll()
    } yield Ok(views.html.category.list(ViewValueCategoryList(
      title  = "Category 一覧",
      cssSrc = Seq("main.css"),
      jsSrc = Seq("main.js"),
      categories = categories
    )))
  }}

  /*
   * カテゴリー登録用
  */
  implicit val colorFormatter = new Formatter[Color] {
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Color] = {
      data.get(key) match {
        case None => Left(Seq(FormError(key, "required")))
        case Some(v) => Try(Color.decode(v)) match {
          case Success(c) => Right(c)
          case Failure(_) => Left(Seq(FormError(key, "required")))
        }
      }
    }
    def unbind(key: String, value: Color): Map[String, String] = {
      Map(key -> ("#" ++ value.getRGB.toHexString.substring(2)))
    }
  }
  val form: Form[CategoryFormData] = Form(
    mapping(
      "name"  -> nonEmptyText(),
      "slug"  -> nonEmptyText(),
      "color" -> of[Color]
    )(CategoryFormData.apply)(CategoryFormData.unapply)
  )

  def add() = Action.async { implicit request: Request[AnyContent] => {
    Future.successful(Ok(views.html.category.store(ViewValueCategoryAdd(
      title  = "Category 追加",
      cssSrc = Seq("main.css"),
      jsSrc  = Seq("main.js"),
      form   = form
    ))))
  }}

  def store() = Action.async { implicit request: Request[AnyContent] => {
    form.bindFromRequest().fold(
      (formWithErrors: Form[CategoryFormData]) => {
        Future.successful(BadRequest(views.html.category.store(ViewValueCategoryAdd(
          title  = "Category 追加",
          cssSrc = Seq("main.css"),
          jsSrc  = Seq("main.js"),
          form   = formWithErrors
        ))))
      },
      (categoryFormData: CategoryFormData) => {
        for {
          result <- default.CategoryRepository.add(Category.apply(
            name  = categoryFormData.name,
            slug  = categoryFormData.slug,
            color = categoryFormData.color
          ))
        } yield (
          Redirect(routes.CategoryController.list())
        )
      }
    )
  }}
}