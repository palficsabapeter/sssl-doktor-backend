package hu.bme.sch.sssl.doktor.auth

import cats.data.EitherT
import cats.implicits._
import hu.bme.sch.sssl.doktor.app.Config.JwtConf
import hu.bme.sch.sssl.doktor.auth.JwtService.JwtPayload
import hu.bme.sch.sssl.doktor.util.ErrorUtil._

import scala.concurrent.{ExecutionContext, Future}

class JwtAuth(
    implicit
    service: JwtService,
    jwtConf: JwtConf,
) {
  def auth(token: String)(
      implicit
      ec: ExecutionContext,
  ): AppErrorOr[JwtPayload] =
    EitherT.fromEither[Future](service.validateAndDecode(token).toRight(AuthError("Invalid JWT token!")))

  implicit class AuthEitherTHelper(decoded: AppErrorOr[JwtPayload]) {
    def withoutPayload[A](onErrorStatus: String => AppError)(block: => AppErrorOr[A])(
        implicit
        executionContext: ExecutionContext,
    ): AppErrorOr[A] =
      withPayload(onErrorStatus)(_ => block)

    def withPayload[A](onErrorStatus: String => AppError)(block: JwtPayload => AppErrorOr[A])(
        implicit
        executionContext: ExecutionContext,
    ): AppErrorOr[A] =
      decoded.flatMap(pl => block(pl).leftMap(err => onErrorStatus(err.message)))
  }
}
