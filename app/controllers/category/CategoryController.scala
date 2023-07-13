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

import model.{ViewValueCategoryList, ViewValueCategoryAdd, ViewValueCategoryEdit}
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

  def delete() = Action.async { implicit request: Request[AnyContent] => {
    request.body.asFormUrlEncoded.get("id").headOption match {
      case Some(id) =>
        default.CategoryRepository
          .remove(Category.Id(id.toLong))
          .flatMap(res =>
            res match {
              case Some(x) => {
                // カテゴリを削除したらそのカテゴリの todo も全て削除する
                default.TodoRepository
                  .getAll()  // todo を全て取得
                  .map(_.collect{
                    case todo if todo.v.categoryId == x.id => default.TodoRepository.remove(todo.id)  // categoryId が合致するものを削除
                  })
                  .flatMap(seqFuture => Future.sequence(seqFuture)) // Seq[Future[Option[lib.model.Todo#EmbeddedId]]] -> Future[Seq[Option[Todo#EmbeddedId]]]]
                  .map(seqOption => if(seqOption.contains(None)) {
                    NotFound(views.html.error.page404())
                  } else {
                    Redirect(routes.CategoryController.list())
                  })
              }
              case None => Future.successful{NotFound(views.html.error.page404())}
            }
          )
      case None => Future.successful{
        NotFound(views.html.error.page404())
      }
    }
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
        for {
          old <- default.CategoryRepository.get(Category.Id(id))
          res <- default.CategoryRepository.update(old.get.map(_.copy(
            name       = editFormData.name,
            slug       = editFormData.slug,
            color      = editFormData.color,
            updatedAt  = java.time.LocalDateTime.now()
          )))
        } yield (
          res match {
            case Some(_) => Redirect(routes.CategoryController.list())
            case None => NotFound(views.html.error.page404())
          }
        )
      }
    )
  }}
}