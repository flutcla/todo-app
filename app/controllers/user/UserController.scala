package controllers.user

import lib.model.{ User, UserPassword }
import lib.persistence.default.{ UserRepository, UserPasswordRepository }
import mvc.auth.UserAuthProfile

import javax.inject._
import play.api.mvc._
import play.api.data._
import play.api.i18n.I18nSupport
import scala.concurrent._

import cats.data.{ EitherT, OptionT }
import cats.implicits._
import ixias.play.api.auth.mvc.AuthExtensionMethods

import play.api.libs.json.Json
import json.reads.{ JsValueUserSignup, JsValueUserLogin }


@Singleton
class UserController @Inject()(
  val controllerComponents: ControllerComponents,
  val authProfile:          UserAuthProfile,
) (implicit ec: ExecutionContext) extends AuthExtensionMethods
  with BaseController
  with I18nSupport
{
  def signup() = Action(parse.json).async { implicit req =>
    req.body
        .validate[JsValueUserSignup]
        .fold(
      errors => Future.successful(BadRequest(Json.toJson("message" -> "The format is wrong."))),
      post => EitherT[Future, Result, JsValueUserSignup](
        for {
          userOpt <- UserRepository.getByEmail(post.email)
        } yield userOpt match {
          case None       => Right(post)
          case Some(user) => Left(BadRequest(Json.toJson("message" -> "The email address is already used.")))
        }
      ) semiflatMap {
        // Email が未登録なら登録する
        case data => for {
          uid    <- UserRepository.add(User(None, data.name, data.email).toWithNoId)
          _      <- UserPasswordRepository.insert(UserPassword.build(uid, data.password))
          // 認証トークンを付与
          result <- authProfile.loginSucceeded(uid, {_ =>
            // TODO:
            Ok(Json.toJson("message" -> (s"User ${uid}:${data.name} Created.")))
          })
        } yield result
      }
    )
  }

  def login() = Action(parse.json).async { implicit req =>
    req.body
        .validate[JsValueUserLogin]
        .fold (
      errors => Future.successful(BadRequest(Json.toJson("message" -> "The format is wrong."))),
      post => OptionT {
        // ユーザーが存在するか取得
        UserRepository.getByEmail(post.email)
      } semiflatMap {
        // パスワードのチェック
        case user => for {
          passwordOpt <- UserPasswordRepository.get(user.id)
          if passwordOpt.exists(_.v.verify(post.password))
        } yield user.id
      } semiflatMap {
        // トークンを付与
        case uid => authProfile.loginSucceeded(uid, _ => {
          // TODO:
          Ok(Json.toJson("message" -> (s"Logged in as ${uid}:${post.email}")))
        })
      } toRight (
        BadRequest(Json.toJson("message" -> "Login failed."))
      )
    )
  }

  def logout() = Authenticated(authProfile).async { implicit req =>
    authProfile.loggedIn { user =>
      authProfile.logoutSucceeded(user.id, {
        // TODO:
        Ok(Json.toJson("message" -> (s"Successfully logged out.")))
      })
    }
  }
}