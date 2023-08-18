package json.writes

import play.api.libs.json._
import lib.model.{Todo, Category}

case class JsValueTodoWithCategory(
  todo:     Todo#EmbeddedId,
  category: Category#EmbeddedId
)

object JsValueTodoWithCategory {
  implicit val todoWithCategoryWrites = Writes[JsValueTodoWithCategory] { todoWithCategory =>
    Json.obj(
      "todo"     -> JsValueTodo(todoWithCategory.todo),
      "category" -> JsValueCategory(todoWithCategory.category)
    )
  }

  def apply(arg: Tuple2[Todo#EmbeddedId, Category#EmbeddedId]): JsValueTodoWithCategory =
    arg match {
      case (todo, category) =>
      JsValueTodoWithCategory(
        todo     = todo,
        category = category
      )
    }
}
