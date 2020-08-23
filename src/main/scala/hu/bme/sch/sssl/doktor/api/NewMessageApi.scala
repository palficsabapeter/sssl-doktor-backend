package hu.bme.sch.sssl.doktor.api

import java.util.UUID

import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.{Authorities, MessageStatus}
import hu.bme.sch.sssl.doktor.api.ApiBase.EmptyResponseBody
import hu.bme.sch.sssl.doktor.auth.JwtAuth
import hu.bme.sch.sssl.doktor.service.NewMessageService.CreateMessageDto
import hu.bme.sch.sssl.doktor.service.{NewMessageService, TicketDetailsService}

import scala.concurrent.ExecutionContext

class NewMessageApi(
    implicit
    jwtAuth: JwtAuth,
    newMessageService: NewMessageService,
    ticketDetailsService: TicketDetailsService,
    ec: ExecutionContext,
) extends ApiBase {
  import JwtAuth._
  import NewMessageApi._
  import io.circe.generic.auto._
  import sttp.tapir._
  import sttp.tapir.json.circe._

  def endpoints =
    List(
      createMessage,
    )

  def createMessage =
    endpoint.post
      .in("tickets" / path[UUID]("ticketId") / "messages")
      .in(auth.bearer)
      .in(jsonBody[MessageTextDto])
      .out(jsonBody[EmptyResponseBody])
      .withGeneralErrorHandler()
      .serverLogic {
        case (ticketId: UUID, bearer: String, message: MessageTextDto) =>
          jwtAuth
            .auth(bearer)
            .withPayload { jwt =>
              val auths     = jwt.authorities
              val createDto = CreateMessageDto(ticketId, jwt.uid, jwt.user, MessageStatus.Shown, message.message)
              if (auths.contains(Authorities.Admin))
                newMessageService.createMessage(createDto)
              else if (auths.contains(Authorities.Clerk))
                ifCreatedByThenCreate(createDto, ticketId, jwt.uid).leftFlatMap { _ =>
                  // so the clerks can create messages to tickets created by them and assigned to them
                  ifAssignedToThenCreate(createDto, ticketId, jwt.uid)
                }
              else
                ifCreatedByThenCreate(createDto, ticketId, jwt.uid)
            }
            .handleUnit
            .value
      }

  private def ifAssignedToThenCreate(dto: CreateMessageDto, ticketId: UUID, uid: String) =
    for {
      _   <- ticketDetailsService.isAssignedToUid(ticketId, uid)
      res <- newMessageService.createMessage(dto.copy(status = MessageStatus.Unreviewed))
    } yield res

  private def ifCreatedByThenCreate(dto: CreateMessageDto, ticketId: UUID, uid: String) =
    for {
      _   <- ticketDetailsService.isCreatedByUid(ticketId, uid)
      res <- newMessageService.createMessage(dto)
    } yield res
}

object NewMessageApi {
  case class MessageTextDto(
      message: String,
  )
}
