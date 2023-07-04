package lib.model

import ixias.model._
import ixias.util.EnumStatus

import java.time.LocalDateTime

// カテゴリーを表すモデル
//~~~~~~~~~~~~~~~~~~~~~
import Category._
case class Category(
  id:        Option[Id],
  name:      String,
  slug:      String,
  color:     Color,
  updatedAt: LocalDateTime,
  createdAt: LocalDateTime
) extends EntityModel[Id]

object Category {
  val  Id = the[Identity[Id]]
  type Id = Long @@ Category
  type WithNoId = Entity.WithNoId[Id, Category]
  type EmbeddedId = Entity.EmbeddedId[Id, Category]

  sealed abstract class Color(val code: Short, val name: String) extends EnumStatus
  object Color extends EnumStatus.Of[Color] {
    case object RED    extends Color(code=0, name="coral")
    case object GREEN  extends Color(code=1, name="lightgreen")
    case object BLUE   extends Color(code=2, name="aliceblue")
    case object ORANGE extends Color(code=3, name="orange")
  }

  def apply(
    name:  String,
    slug:  String,
    color: Color
  ): WithNoId = {
    new Entity.WithNoId(
      new Category(
        id          = None,
        name        = name,
        slug        = slug,
        color       = color,
        updatedAt   = java.time.LocalDateTime.now(),
        createdAt   = java.time.LocalDateTime.now()
      )
    )
  }
}