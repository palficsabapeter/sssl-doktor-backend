package hu.bme.sch.sssl.doktor.api

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.Authorities
import hu.bme.sch.sssl.doktor.api.ApiBase.EmptyResponseBody
import hu.bme.sch.sssl.doktor.auth.JwtAuth
import hu.bme.sch.sssl.doktor.service.AssignTicketService
import hu.bme.sch.sssl.doktor.util.ErrorUtil.AuthError

import scala.concurrent.{ExecutionContext, Future}

class AssignTicketApi(
    implicit
    jwtAuth: JwtAuth,
    service: AssignTicketService,
    ec: ExecutionContext,
) extends ApiBase {
  import JwtAuth._
  import io.circe.generic.auto._
  import sttp.tapir._
  import sttp.tapir.json.circe._

  def endpoints =
    List(
      assignTicket,
      unassignTicket,
    )

  private def assignTicket =
    endpoint.post
      .in("tickets" / path[UUID] / "assign" / path[String])
      .in(auth.bearer)
      .out(jsonBody[EmptyResponseBody])
      .withGeneralErrorHandler()
      .serverLogic {
        case (ticket: UUID, uid: String, bearer: String) =>
          jwtAuth
            .auth(bearer)
            .withPayload { jwt =>
              val auths = jwt.authorities
              if (auths.contains(Authorities.Admin) || (auths.contains(Authorities.Clerk) && jwt.uid == uid))
                service.assignTicket(ticket, uid)
              else
                EitherT.leftT[Future, Unit](AuthError("No sufficient authorities!"))
            }
            .handleUnit
            .value
      }

  private def unassignTicket =
    endpoint.post
      .in("tickets" / path[UUID] / "unassign")
      .in(auth.bearer)
      .out(jsonBody[EmptyResponseBody])
      .withGeneralErrorHandler()
      .serverLogic {
        case (ticket: UUID, bearer: String) =>
          jwtAuth
            .auth(bearer)
            .withPayload { jwt =>
              val auths = jwt.authorities
              if (auths.contains(Authorities.Admin))
                service.unassignTicket(ticket)
              else if (auths.contains(Authorities.Clerk))
                for {
                  _   <- service.isAssignedToClerk(ticket, jwt.uid)
                  res <- service.unassignTicket(ticket)
                } yield res
              else
                EitherT.leftT[Future, Unit](AuthError("No sufficient authorities!"))
            }
            .handleUnit
            .value
      }
}
