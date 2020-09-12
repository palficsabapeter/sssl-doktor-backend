package hu.bme.sch.sssl.doktor.api

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.Authorities
import hu.bme.sch.sssl.doktor.api.ApiBase.EmptyResponseBody
import hu.bme.sch.sssl.doktor.auth.JwtAuth
import hu.bme.sch.sssl.doktor.service.ApproveMessageService
import hu.bme.sch.sssl.doktor.util.ErrorUtil.AuthError

import scala.concurrent.{ExecutionContext, Future}

class ApproveMessageApi(
    implicit
    jwtAuth: JwtAuth,
    service: ApproveMessageService,
    ec: ExecutionContext,
) extends ApiBase {
  import JwtAuth._
  import io.circe.generic.auto._
  import sttp.tapir._
  import sttp.tapir.json.circe._

  def endpoints =
    List(
      approveMessage,
      declineMessage,
    )

  private def approveMessage =
    endpoint.post
      .in("tickets" / path[UUID] / "messages" / path[UUID] / "approve")
      .in(auth.bearer)
      .out(jsonBody[EmptyResponseBody])
      .withGeneralErrorHandler()
      .serverLogic {
        case (ticketId: UUID, messageId: UUID, bearer: String) =>
          jwtAuth
            .auth(bearer)
            .withPayload { jwt =>
              if (jwt.authorities.contains(Authorities.Admin))
                service.approveMessage(ticketId, messageId, true, jwt.user, jwt.uid)
              else
                EitherT.leftT[Future, Unit](AuthError("No sufficient authorities!"))
            }
            .handleUnit
            .value
      }

  private def declineMessage =
    endpoint.post
      .in("tickets" / path[UUID] / "messages" / path[UUID] / "decline")
      .in(auth.bearer)
      .out(jsonBody[EmptyResponseBody])
      .withGeneralErrorHandler()
      .serverLogic {
        case (ticketId: UUID, messageId: UUID, bearer: String) =>
          jwtAuth
            .auth(bearer)
            .withPayload { jwt =>
              if (jwt.authorities.contains(Authorities.Admin))
                service.approveMessage(ticketId, messageId, false, jwt.user, jwt.uid)
              else
                EitherT.leftT[Future, Unit](AuthError("No sufficient authorities!"))
            }
            .handleUnit
            .value
      }
}
