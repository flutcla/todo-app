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

  /*
   * カテゴリー登録用
  */
  implicit val colorFormatter = new Formatter[Color] {
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Color] = {
      data.get(key) match {
        case None => Left(Seq(FormError(key, "required")))
        case Some(v) => Right(Color.decode(v))
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

  def delete() = Action.async { implicit request: Request[AnyContent] => {
    request.body.asFormUrlEncoded.get("id").headOption match {
      case Some(id) =>
        for {
          res <- default.CategoryRepository.remove(Category.Id(id.toLong))
        } yield (
          res match {
            case Some(x) => {
              Redirect(routes.CategoryController.list())
            }
            case None => NotFound(views.html.error.page404())
          }
        )
      case None => Future.successful{
        NotFound(views.html.error.page404())
      }
    }
  }}
}