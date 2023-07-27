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
    } yield Ok(Json.toJson(categories.map(json.writes.JsValueCategory.apply _)))
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

  def store() = Action(parse.json).async { implicit request => {
    request.body
      .validate[JsValueCategoryStore]
      .fold(
        errors => Future.successful(BadRequest(Json.toJson("message" -> "The format is wrong."))),
        categoryStoreData => for {
          result <- default.CategoryRepository.add(Category.apply(
            name  = categoryStoreData.name,
            slug  = categoryStoreData.slug,
            color = categoryStoreData.color
          ))
        } yield (
          Redirect(routes.CategoryController.list())
        )
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

  def update(id: Long) = Action(parse.json).async { implicit request => {
    request.body
      .validate[json.reads.JsValueCategory]
      .fold(
        errors => Future.successful(BadRequest(Json.toJson("message" -> "The format is wrong."))),
        categoryData => {
          val ot: OptionT[Future, play.api.mvc.Result] = for {
            old <- OptionT(default.CategoryRepository.get(Category.Id(id)))
            updateData = old.map(_.copy(
              name  = categoryData.name,
              slug  = categoryData.slug,
              color = categoryData.color
            ))
            _ <- OptionT(default.CategoryRepository.update(updateData))
          } yield Redirect(routes.CategoryController.list())
          ot.getOrElse(NotFound(Json.toJson("message" -> "")))
        }
      )
  }}
}