package hu.bme.sch.sssl.doktor.api

import hu.bme.sch.sssl.doktor.`enum`.TicketType.TicketType
import hu.bme.sch.sssl.doktor.api.ApiBase._
import hu.bme.sch.sssl.doktor.auth.JwtAuth
import hu.bme.sch.sssl.doktor.service.NewTicketService
import hu.bme.sch.sssl.doktor.util.ErrorUtil._

import scala.concurrent.ExecutionContext

class TicketsApi(
    implicit
    jwtAuth: JwtAuth,
    service: NewTicketService,
    ec: ExecutionContext,
) extends ApiBase {
  import JwtAuth._
  import TicketsApi._
  import io.circe.generic.auto._
  import sttp.tapir._
  import sttp.tapir.json.circe._

  def endpoints =
    List(
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
              .withPayload(jwt => service.createTicket(jwt.uid, jwt.user, dto))
              .handleUnit
              .value
        },
    )
}

object TicketsApi {
  case class CreateTicketDto(
      ticketType: TicketType,
      isAnonym: Boolean,
      description: String,
  )
}
