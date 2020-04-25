package hu.bme.sch.sssl.doktor.api

import cats.implicits._
import hu.bme.sch.sssl.doktor.util.ErrorUtil._
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{jsonBody, oneOf, statusMapping, Endpoint}

import scala.concurrent.{ExecutionContext, Future}

trait ApiBase {
  import ApiBase._

  def endpoints: List[ServerEndpoint[_, _, _, Nothing, Future]]

  implicit class UnitReturnTypeMapper(x: AppErrorOr[Unit]) {
    def handleUnit(
        implicit
        executionContext: ExecutionContext,
    ): AppErrorOr[EmptyResponseBody] = x.map(_ => EmptyResponseBody())
  }

  implicit class ErrorHandlerHelper[A, C, D](se: Endpoint[A, Unit, C, D]) {
    import io.circe.generic.auto._
    import sttp.tapir.json.circe._
    def withGeneralErrorHandler(): Endpoint[A, AppError, C, D] =
      se.errorOut(
        oneOf[AppError](
          statusMapping(StatusCode.Unauthorized, jsonBody[AuthError].description("Unauthorized!")),
          statusMapping(StatusCode.ServiceUnavailable, jsonBody[DbUnavailable].description("Database unavailable!")),
          statusMapping(StatusCode.BadRequest, jsonBody[UnsuccessfulAction].description("Unsuccessful action!")),
        ),
      )
  }
}

object ApiBase {
  case class EmptyResponseBody()
}
