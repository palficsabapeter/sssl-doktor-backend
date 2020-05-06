package hu.bme.sch.sssl.doktor.api

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.Authorities
import hu.bme.sch.sssl.doktor.api.ApiBase.EmptyResponseBody
import hu.bme.sch.sssl.doktor.auth.JwtAuth
import hu.bme.sch.sssl.doktor.service.ChangeTicketStatusService
import hu.bme.sch.sssl.doktor.util.ErrorUtil.AuthError

import scala.concurrent.{ExecutionContext, Future}

class TicketApi(
    implicit
    jwtAuth: JwtAuth,
    service: ChangeTicketStatusService,
    ec: ExecutionContext,
) extends ApiBase {
  import JwtAuth._
  import io.circe.generic.auto._
  import sttp.tapir._
  import sttp.tapir.json.circe._

  def endpoints =
    List(
      openTicket,
      closeTicket,
    )

  private def openTicket =
    endpoint.post
      .in("tickets" / path[UUID]("ticketId") / "open")
      .in(auth.bearer)
      .out(jsonBody[EmptyResponseBody])
      .withGeneralErrorHandler()
      .serverLogic {
        case (ticketId: UUID, bearer: String) =>
          jwtAuth
            .auth(bearer)
            .withPayload { jwt =>
              if (jwt.authorities.contains(Authorities.Admin))
                service.changeTicketStatus(ticketId, true)
              else
                EitherT.leftT[Future, Unit](AuthError("No sufficient authorities!"))
            }
            .handleUnit
            .value
      }

  private def closeTicket =
    endpoint.post
      .in("tickets" / path[UUID]("ticketId") / "close")
      .in(auth.bearer)
      .out(jsonBody[EmptyResponseBody])
      .withGeneralErrorHandler()
      .serverLogic {
        case (ticketId: UUID, bearer: String) =>
          jwtAuth
            .auth(bearer)
            .withPayload { jwt =>
              if (jwt.authorities.contains(Authorities.Admin))
                service.changeTicketStatus(ticketId, false)
              else
                EitherT.leftT[Future, Unit](AuthError("No sufficient authorities!"))
            }
            .handleUnit
            .value
      }
}
