package hu.bme.sch.sssl.doktor.api

import cats.implicits._
import hu.bme.sch.sssl.doktor.util.ErrorUtil._
import sttp.tapir.Endpoint
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{jsonBody, oneOf, statusDefaultMapping, statusMapping}

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
    import sttp.tapir.json.circe._
    import io.circe.generic.auto._
    def withGeneralErrorHandler(): Endpoint[A, AppError, C, D] =
      se.errorOut(
        oneOf[AppError](
          statusMapping(StatusCode.Unauthorized, jsonBody[AuthError].description("Unauthorized!")),
          statusMapping(StatusCode.BadRequest, jsonBody[UnsuccessfulAction].description("Unsuccessful action!")),
        ),
      )
  }
}

object ApiBase {
  case class EmptyResponseBody()
}
