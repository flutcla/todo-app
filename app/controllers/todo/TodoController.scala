package controllers.todo

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import play.api.mvc.BaseController
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.data.FormError
import play.api.data.format.{ Formats, Formatter }

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

import model.{ViewValueTodo, ViewValueTodoAdd, ViewValueTodoEdit}
import lib.model.Todo
import lib.model.Category
import lib.persistence.default

case class TodoFormData(
  categoryId : Category.Id,
  title :      String,
  body :       String
)

case class TodoEditFormData(
  categoryId : Category.Id,
  title :      String,
  body :       String,
  state:       Todo.Status
)

@Singleton
class TodoController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with I18nSupport {
  def list() = Action.async { implicit request: Request[AnyContent] => {
    val todoFuture = default.TodoRepository.getAll()
    val categoryFuture = default.CategoryRepository.getAll()
    for {
      todos <- todoFuture
      categories <- categoryFuture
    } yield {
      val todoCategoryOptSeq: Seq[Option[Tuple2[Todo#EmbeddedId, Category#EmbeddedId]]] = todos.map(res =>
          categories.collectFirst{
            case cat if cat.id == res.v.categoryId => (res, cat)
          }
        )
      if (todoCategoryOptSeq.contains(None)) {
        NotFound(views.html.error.page404())
      } else {
        Ok(views.html.todo.list(ViewValueTodo(
          title  = "Todo 一覧",
          cssSrc = Seq("main.css"),
          jsSrc = Seq("main.js"),
          todoCategorySeq = todoCategoryOptSeq.flatten)))
      }
    }
  }}

  // 登録用
  implicit val categoryIdFormatter = new Formatter[Category.Id] {
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Category.Id] =
      Formats.intFormat.bind(key, data).right.map(Category.Id(_))
    def unbind(key: String, value: Category.Id): Map[String, String] =
      Map(key -> value.toString)
  }
  val form: Form[TodoFormData] = Form(
    mapping(
      "categoryId" -> of[Category.Id],
      "title"      -> nonEmptyText(maxLength = 140),
      "body"       -> nonEmptyText()
    )(TodoFormData.apply)(TodoFormData.unapply(_))
  )

  def add() = Action.async { implicit request: Request[AnyContent] => {
    for {
      categories <- default.CategoryRepository.getAll()
    } yield (
      Ok(views.html.todo.store(ViewValueTodoAdd(
        title      = "Todo 追加",
        cssSrc     = Seq("main.css"),
        jsSrc      = Seq("main.js"),
        form       = form,
        categories = categories
      )))
    )
  }}

  def store() = Action.async { implicit request: Request[AnyContent] => {
    form.bindFromRequest().fold(
      (formWithErrors: Form[TodoFormData]) => {
        for {
          categories <- default.CategoryRepository.getAll()
        } yield (
          BadRequest(views.html.todo.store(ViewValueTodoAdd(
            title      = "Todo 追加",
            cssSrc     = Seq("main.css"),
            jsSrc      = Seq("main.js"),
            form       = form,
            categories = categories
          )))
        )
      },
      (todoFormData: TodoFormData) => {
        for {
          result <- default.TodoRepository.add(Todo.apply(
            categoryId = Category.Id(todoFormData.categoryId),
            title      = todoFormData.title,
            body       = todoFormData.body,
            state      = Todo.Status.TODO
          ))
        } yield (
          Redirect(routes.TodoController.list())
        )
      }
    )
  }}

  /**
  * 対象のデータを削除する
  */
  def delete() = Action.async { implicit request: Request[AnyContent] => {
    request.body.asFormUrlEncoded.get("id").headOption match {
      case Some(id) =>
        for {
          res <- default.TodoRepository.remove(Todo.Id(id.toLong))
        } yield (
          res match {
            case Some(x) => {
              Redirect(routes.TodoController.list())
            }
            case None => NotFound(views.html.error.page404())
          }
        )
      case None => Future{
        NotFound(views.html.error.page404())
      }
    }
  }}

  /**
   * 編集画面
   */
  implicit val todoStatusFormatter = new Formatter[Todo.Status] {
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Todo.Status] =
      Formats.shortFormat.bind(key, data).right.map(Todo.Status(_))
    def unbind(key: String, value: Todo.Status): Map[String, String] =
      Map(key -> value.toString)
  }

  val editForm = Form(
    mapping(
      "categoryId" -> of[Category.Id],
      "title"      -> nonEmptyText(maxLength = 140),
      "body"       -> nonEmptyText(),
      "state"      -> of[Todo.Status]
    )(TodoEditFormData.apply)(TodoEditFormData.unapply(_))
  )

  def edit(id: Long) = Action.async { implicit request: Request[AnyContent] => {
    for {
      res <- default.TodoRepository.get(Todo.Id(id))
      categories <- default.CategoryRepository.getAll()
    } yield (
      res match {
        case Some(todo) => Ok(views.html.todo.edit(
          ViewValueTodoEdit(
            title = "Todo 追加",
            cssSrc = Seq("main.css"),
            jsSrc = Seq("main.js"),
            id = Todo.Id(id),
            form = editForm.fill(TodoEditFormData(
              todo.v.categoryId,
              todo.v.title,
              todo.v.body,
              todo.v.state
            )),
            categories = categories,
            status = Todo.Status.values
          )
        ))
        case None => NotFound(views.html.error.page404())
      }
    )
  }}

  def update(id: Long) = Action.async { implicit request: Request[AnyContent] => {
    editForm.bindFromRequest().fold(
      (formWithErrors: Form[TodoEditFormData]) => {
        for {
          categories <- default.CategoryRepository.getAll()
        } yield (
          BadRequest(views.html.todo.edit(
            ViewValueTodoEdit(
              title = "Todo 追加",
              cssSrc = Seq("main.css"),
              jsSrc = Seq("main.js"),
              id = Todo.Id(id),
              form = formWithErrors,
              categories = categories,
              status = Todo.Status.values
            )
          ))
        )
      },
      (editFormData: TodoEditFormData) => {
        default.TodoRepository.get(Todo.Id(id)).flatMap(old =>
          old match {
            case Some(todo) => for {
              res <- default.TodoRepository.update(todo.map(_.copy(
                categoryId = editFormData.categoryId,
                title      = editFormData.title,
                body       = editFormData.body,
                state      = editFormData.state,
                updatedAt  = java.time.LocalDateTime.now()
              )))
            } yield (
              res match {
                case Some(_) => Redirect(routes.TodoController.list())
                case None => NotFound(views.html.error.page404())
              }
            )
            case None => Future.successful{NotFound(views.html.error.page404())}
          }
        )
      }
    )
  }}
}
