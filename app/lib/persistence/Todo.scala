package lib.persistence

import scala.concurrent.Future
import ixias.persistence.SlickRepository
import lib.model.{Todo, Category}
import slick.jdbc.JdbcProfile

// TodoRepository: TodoTable へのクエリ発行を行う　Repository 層
//~~~~~~~~~~~~~~~~~~~~~
case class TodoRepository[P <: JdbcProfile]()(implicit val driver: P)
  extends SlickRepository[Todo.Id, Todo, P]
  with db.SlickResourceProvider[P] {

  import api._

  /**
    * Get Todo Data
    */
  def get(id: Id): Future[Option[EntityEmbeddedId]] =
    RunDBAction(TodoTable, "slave") { _
      .filter(_.id === id)
      .result.headOption
    }

  /**
    * Get All Todo Data
    */
  def getAll(): Future[Seq[EntityEmbeddedId]] =
    RunDBAction(TodoTable, "slave") {
      _.result
    }

  /**
    * Add Todo Data
   */
  def add(entity: EntityWithNoId): Future[Id] =
    RunDBAction(TodoTable) { slick =>
      slick returning slick.map(_.id) += entity.v
    }

  /**
   * Update Todo Data
   */
  def update(entity: EntityEmbeddedId): Future[Option[EntityEmbeddedId]] =
    RunDBAction(TodoTable) { slick =>
      val row = slick.filter(_.id === entity.id)
      for {
        old <- row.result.headOption
        _   <- old match {
          case None    => DBIO.successful(0)
          case Some(_) => row.update(entity.v)
        }
      } yield old
    }

  /**
   * Delete Todo Data
   */
  def remove(id: Id): Future[Option[EntityEmbeddedId]] =
    RunDBAction(TodoTable) { slick =>
      val row = slick.filter(_.id === id)
      for {
        old <- row.result.headOption
        _   <- old match {
          case None    => DBIO.successful(0)
          case Some(_) => row.delete
        }
      } yield old
    }

  /**
   * Delete Todo Data By Category Id
   */
  def removeByCategoryId(categoryId: Category.Id): Future[Seq[EntityEmbeddedId]] =
    RunDBAction(TodoTable) { slick =>
      val row = slick.filter(_.categoryId === categoryId)
      for {
        oldSeq <- row.result
        _ <- row.delete
      } yield oldSeq
    }
}