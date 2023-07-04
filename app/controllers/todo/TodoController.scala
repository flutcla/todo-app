package controllers.todo

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import play.api.mvc.BaseController
import play.api.mvc.Request
import play.api.mvc.AnyContent
import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import model.ViewValueTodo
import lib.model.Todo
import lib.persistence.onMySQL

@Singleton
class TodoController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def list() = Action.async { implicit request: Request[AnyContent] => {
    val vv = ViewValueTodo(
      title  = "Todo 一覧",
      cssSrc = Seq("main.css"),
      jsSrc  = Seq("main.js")
    )
    val todoFuture = onMySQL.TodoRepository.getAll()
    val categoryFuture = onMySQL.CategoryRepository.getAll()
    Await.ready(todoFuture, Duration.Inf)
    Await.ready(categoryFuture, Duration.Inf)
    for {
      results <- todoFuture
      categories <- categoryFuture
    } yield (
      Ok(views.html.todo.list(
          vv,
          results.map(res =>
          (
            res,
            categories
              .filter(_.id == res.v.categoryId)
              .headOption
              .get
          )
        )
      ))
    )
  }}
}
