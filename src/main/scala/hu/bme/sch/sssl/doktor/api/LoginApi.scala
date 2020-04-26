package hu.bme.sch.sssl.doktor.api

import hu.bme.sch.sssl.doktor.service.LoginService
import hu.bme.sch.sssl.doktor.service.LoginService.JwtResponse
import hu.bme.sch.sssl.doktor.util.LoggerUtil
import org.slf4j.Logger

import scala.concurrent.ExecutionContext

class LoginApi(
    implicit
    service: LoginService,
    ec: ExecutionContext,
) extends ApiBase {
  import io.circe.generic.auto._
  import sttp.tapir._
  import sttp.tapir.json.circe._

  implicit val logger: Logger = LoggerUtil.getLogger(getClass)

  def endpoints =
    List(
      endpoint.post
        .in("login")
        .in(query[String]("authorizationCode"))
        .out(jsonBody[JwtResponse])
        .withGeneralErrorHandler()
        .serverLogic(authorizationCode => service.login(authorizationCode).value),
    )
}
