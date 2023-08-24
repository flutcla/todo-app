package mvc.filter

import javax.inject.Inject
import akka.stream.Materializer
import play.api.Logging
import play.api.mvc._
import play.api.mvc.Results._
import play.api.http.Status._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import ixias.play.api.mvc.Errors._

import play.api.libs.json.Json

class ErrorFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext) extends Filter {
  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] =
    nextFilter(requestHeader).map { result =>
      result.header.status match {
        case NOT_FOUND =>
          NotFound(Json.toJson("message" -> "404 Not Found"))
        case UNAUTHORIZED =>
          Unauthorized(Json.toJson("message" -> "401 Unauthorized"))
        case _         =>
          result
      }
    }
}
