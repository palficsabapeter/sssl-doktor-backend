package hu.bme.sch.sssl.doktor.api

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.Authorities
import hu.bme.sch.sssl.doktor.api.ApiBase.EmptyResponseBody
import hu.bme.sch.sssl.doktor.auth.JwtAuth
import hu.bme.sch.sssl.doktor.service.TicketDetailsService.TicketDetailsDto
import hu.bme.sch.sssl.doktor.service.{ChangeTicketStatusService, TicketDetailsService}
import hu.bme.sch.sssl.doktor.util.ErrorUtil.AuthError

import scala.concurrent.{ExecutionContext, Future}

class TicketApi(
    implicit
    jwtAuth: JwtAuth,
    changeTicketStatusService: ChangeTicketStatusService,
    ticketDetailsService: TicketDetailsService,
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
      ticketDetails,
    )

  private def ticketDetails =
    endpoint.get
      .in("tickets" / path[UUID]("ticketId"))
      .in(auth.bearer)
      .out(jsonBody[TicketDetailsDto])
      .withGeneralErrorHandler()
      .serverLogic {
        case (ticketId: UUID, bearer: String) =>
          jwtAuth
            .auth(bearer)
            .withPayload { jwt =>
              if (jwt.authorities.contains(Authorities.Admin))
                ticketDetailsService.getTicketDetails(ticketId, jwt.uid)
              else if (jwt.authorities.contains(Authorities.Clerk))
                ifCreatedByThenGetDetails(ticketId, jwt.uid).leftFlatMap { _ =>
                  // so the clerks can get ticket details for tickets created by them and assigned to them
                  ifAssignedToThenGetDetails(ticketId, jwt.uid)
                }
              else
                ifCreatedByThenGetDetails(ticketId, jwt.uid)
            }
            .value
      }

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
                changeTicketStatusService.changeTicketStatus(ticketId, true)
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
                changeTicketStatusService.changeTicketStatus(ticketId, false)
              else
                EitherT.leftT[Future, Unit](AuthError("No sufficient authorities!"))
            }
            .handleUnit
            .value
      }

  private def ifAssignedToThenGetDetails(ticketId: UUID, uid: String) =
    for {
      _   <- ticketDetailsService.isAssignedToUid(ticketId, uid)
      res <- ticketDetailsService.getTicketDetails(ticketId, uid)
    } yield res

  private def ifCreatedByThenGetDetails(ticketId: UUID, uid: String) =
    for {
      _   <- ticketDetailsService.isCreatedByUid(ticketId, uid)
      res <- ticketDetailsService.getTicketDetails(ticketId, uid)
    } yield res
}
