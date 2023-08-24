package mvc.filter

import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent._
import javax.inject.Singleton

import java.util.NoSuchElementException
import play.api.libs.json.Json

@Singleton
class ErrorHandler extends HttpErrorHandler {
  // クライアントエラー
  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(
      Status(statusCode)(Json.toJson("message" -> s"A client error occurred: ${message}"))
    )
  }

  // サーバーエラー
  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    exception match {
      case e: NoSuchElementException =>
        // None.get などの例外
        Future.successful(NotFound(Json.toJson("message" -> "404 Not Found")))
      case _                         =>
        // それ以外の例外
        Future.successful(InternalServerError(Json.toJson("Message" -> s"Internal Server Error: ${exception.getMessage}")))
    }
  }
}
