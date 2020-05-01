package hu.bme.sch.sssl.doktor.api

import hu.bme.sch.sssl.doktor.`enum`.TicketType.TicketType
import hu.bme.sch.sssl.doktor.api.ApiBase._
import hu.bme.sch.sssl.doktor.auth.JwtAuth
import hu.bme.sch.sssl.doktor.service.MyTicketsService.MyTicketsResponseDto
import hu.bme.sch.sssl.doktor.service.{MyTicketsService, NewTicketService}

import scala.concurrent.ExecutionContext

class TicketsApi(
    implicit
    jwtAuth: JwtAuth,
    newTicketService: NewTicketService,
    myTicketsService: MyTicketsService,
    ec: ExecutionContext,
) extends ApiBase {
  import JwtAuth._
  import TicketsApi._
  import io.circe.generic.auto._
  import sttp.tapir._
  import sttp.tapir.json.circe._

  def endpoints =
    List(
      createTicket,
      listMyTickets,
    )

  private def createTicket =
    endpoint.post
      .in("tickets")
      .in(auth.bearer)
      .in(jsonBody[CreateTicketDto])
      .out(jsonBody[EmptyResponseBody])
      .withGeneralErrorHandler()
      .serverLogic {
        case (bearer: String, dto: CreateTicketDto) =>
          jwtAuth
            .auth(bearer)
            .withPayload(jwt => newTicketService.createTicket(jwt.uid, jwt.user, dto))
            .handleUnit
            .value
      }

  private def listMyTickets =
    endpoint.get
      .in("tickets")
      .in(auth.bearer)
      .out(jsonBody[MyTicketsResponseDto])
      .withGeneralErrorHandler()
      .serverLogic { bearer: String =>
        jwtAuth
          .auth(bearer)
          .withPayload(jwt => myTicketsService.getMyTickets(jwt.uid))
          .value
      }
}

object TicketsApi {
  case class CreateTicketDto(
      ticketType: TicketType,
      isAnonym: Boolean,
      description: String,
  )
}
