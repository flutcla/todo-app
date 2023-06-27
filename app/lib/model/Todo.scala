package lib.model

import ixias.model._
import ixias.util.EnumStatus

import java.time.LocalDateTime

// Todo を表すモデル
import Todo._
case class Todo(
  id:          Option[Id],
  category_id: Long,
  title:       String,
  body:        String,
  state:       Status,
  updatedAt:  LocalDateTime,
  createdAt:  LocalDateTime
) extends EntityModel[Id]

object Todo {
  val  Id = the[Identity[Id]]
  type Id = Long @@ Todo
  type WithNoId = Entity.WithNoId[Id, Todo]
  type EmbeddedId = Entity.EmbeddedId[Id, Todo]

  sealed abstract class Status(val code: Short, val name: String) extends EnumStatus
  object Status extends EnumStatus.Of[Status] {
    case object TODO     extends Status(code=1, name="TODO")
    case object PROGRESS extends Status(code=2, name="進行中")
    case object DONE     extends Status(code=3, name="完了")
  }

  def apply(
    category_id: Long,
    title:       String,
    body:        String,
    state:       Status
  ): WithNoId = {
    new Entity.WithNoId(
      new Todo(
        id          = None,
        category_id = category_id,
        title       = title,
        body        = body,
        state       = state,
        createdAt  = java.time.LocalDateTime.now(),
        updatedAt  = java.time.LocalDateTime.now()
      )
    )
  }
}
