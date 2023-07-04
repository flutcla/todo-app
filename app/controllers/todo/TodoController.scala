package controllers.todo

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import play.api.mvc.BaseController
import play.api.mvc.Request
import play.api.mvc.AnyContent
import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

import model.ViewValueTodo
import lib.model.Todo
import lib.model.Category
import lib.persistence.default

@Singleton
class TodoController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def list() = Action.async { implicit request: Request[AnyContent] => {
    val todoFuture = default.TodoRepository.getAll()
    val categoryFuture = default.CategoryRepository.getAll()
    Await.ready(todoFuture, Duration.Inf)
    Await.ready(categoryFuture, Duration.Inf)
    for {
      todos <- todoFuture
      categories <- categoryFuture
    } yield {
      val todoCategoryOptSeq: Seq[Option[Tuple2[Todo#EmbeddedId, Category#EmbeddedId]]] = todos.map(res =>
        categories.filter(_.id == res.v.categoryId).headOption match {
          case Some(cat) => Some(res, cat)
          case None      => None
        }
      )
      if (todoCategoryOptSeq.contains(None)) {
        ???  // TODO: Category が見つからなかった際の処理を考える
      } else {
        Ok(views.html.todo.list(ViewValueTodo(
          title  = "Todo 一覧",
          cssSrc = Seq("main.css"),
          jsSrc = Seq("main.js"),
          todoCategorySeq = todoCategoryOptSeq.flatten)))
      }
    }
  }}
}
