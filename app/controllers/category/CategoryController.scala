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

import cats.data.OptionT
import cats.implicits._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}
import java.awt.Color

import model.{ViewValueCategoryList, ViewValueCategoryAdd, ViewValueCategoryEdit}
import lib.persistence.default
import lib.model.Category

import json.writes._
import json.reads._
import play.api.libs.json.Json

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
    } yield Ok(Json.toJson(categories.map(JsValueCategory.apply)))
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
      Map(key -> f"#${value.getRGB}%06x")
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

  def delete() = Action(parse.json).async { implicit request => {
    request.body
      .validate[JsValueCategoryDelete]
      .fold(
        errors => Future.successful(BadRequest(Json.toJson("message" -> "The format is wrong."))),
        categoryDeleteData => for {
          res <- default.CategoryRepository.remove(categoryDeleteData.id)
          _ <- default.TodoRepository.removeByCategoryId(categoryDeleteData.id)
        } yield (
          res match {
            case Some(x) => {
              Ok(Json.toJson("message" -> s"Successfully deleted ${categoryDeleteData.id.toLong}."))
            }
            case None => NotFound(Json.toJson("message" -> s"ID ${categoryDeleteData.id.toLong} does not exist."))
          }
        )
      )
  }}

  def edit(id: Long) = Action.async { implicit request: Request[AnyContent] => {
    for {
      res <- default.CategoryRepository.get(Category.Id(id))
    } yield (
      res match {
        case Some(category) => Ok(views.html.category.edit(
          ViewValueCategoryEdit(
            title = "Category 編集",
            cssSrc = Seq("main.css"),
            jsSrc = Seq("main.js"),
            id = Category.Id(id),
            form = form.fill(CategoryFormData(
              category.v.name,
              category.v.slug,
              category.v.color,
            )),
          )
        ))
        case None => NotFound(views.html.error.page404())
      }
    )
  }}

  def update(id: Long) = Action.async { implicit request: Request[AnyContent] => {
    form.bindFromRequest().fold(
      (formWithErrors: Form[CategoryFormData]) => {
        for {
          categories <- default.CategoryRepository.getAll()
        } yield (
          BadRequest(views.html.category.edit(
            ViewValueCategoryEdit(
              title = "Todo 追加",
              cssSrc = Seq("main.css"),
              jsSrc = Seq("main.js"),
              id = Category.Id(id),
              form = formWithErrors
            )
          ))
        )
      },
      (editFormData: CategoryFormData) => {
        val ot: OptionT[Future, play.api.mvc.Result] = for {
          category <- OptionT(default.CategoryRepository.get(Category.Id(id))) // Future[Option[EntityEmbeddedId]]
          updateData = category.map(_.copy(
              name       = editFormData.name,
              slug       = editFormData.slug,
              color      = editFormData.color,
              updatedAt  = java.time.LocalDateTime.now()
            ))
          _ <- OptionT(default.CategoryRepository.update(updateData))
        } yield Redirect(routes.CategoryController.list())
        ot.getOrElse(NotFound(views.html.error.page404()))
      }
    )
  }}
}