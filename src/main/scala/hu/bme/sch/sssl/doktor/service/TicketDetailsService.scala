package hu.bme.sch.sssl.doktor.service

import java.util.{Locale, UUID}

import cats.data.EitherT
import cats.implicits._
import com.osinka.i18n.{Lang, Messages}
import hu.bme.sch.sssl.doktor.`enum`.MessageStatus.MessageStatus
import hu.bme.sch.sssl.doktor.`enum`.TicketType.TicketType
import hu.bme.sch.sssl.doktor.app.Config.LangConf
import hu.bme.sch.sssl.doktor.repository.{MessageRepository, TicketRepository}
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AppError, AppErrorOr, AuthError, TicketNotFound}

import scala.concurrent.{ExecutionContext, Future}

class TicketDetailsService(
    implicit
    ticketRepo: TicketRepository,
    messageRepo: MessageRepository,
    langConf: LangConf,
    ec: ExecutionContext,
) {
  import TicketDetailsService._

  implicit val lang: Lang = Lang(new Locale(langConf.locale))

  def getTicketDetails(ticketId: UUID, uid: String): AppErrorOr[TicketDetailsDto] =
    for {
      ticket   <- EitherT.fromOptionF(ticketRepo.findById(ticketId), noTicketFoundError(ticketId))
      messages <- EitherT.right(messageRepo.findByTicketId(ticketId))
    } yield {
      val keepTicketAnonimity = ticket.isAnonym && ticket.uid != uid
      val messageDtos = messages.map { message =>
        val keepMessageAnonimity = keepTicketAnonimity && message.uid == ticket.uid
        MessageDto(
          message.messageId,
          if (keepMessageAnonimity) getAnonymText else message.createdBy,
          message.createdAt,
          message.status,
          message.text,
          if (ticket.uid == uid) None else message.reviewedBy,
          if (ticket.uid == uid) None else message.reviewedAt,
        )
      }.toIndexedSeq

      TicketDetailsDto(
        ticket.ticketId,
        if (keepTicketAnonimity) getAnonymText else ticket.createdBy,
        ticket.createdAt,
        ticket.ticketType,
        ticket.description,
        ticket.assignedTo,
        messageDtos,
      )
    }

  def isCreatedByUid(ticketId: UUID, uid: String): AppErrorOr[Unit] =
    for {
      ticket <- EitherT.fromOptionF(ticketRepo.findById(ticketId), noTicketFoundError(ticketId))
      res    <- EitherT.cond[Future](ticket.uid == uid, {}, insufficientAuthError(uid))
    } yield res

  def isAssignedToUid(ticketId: UUID, uid: String): AppErrorOr[Unit] =
    for {
      ticket   <- EitherT.fromOptionF(ticketRepo.findById(ticketId), noTicketFoundError(ticketId))
      assigned <- EitherT.fromOption[Future](ticket.assignedTo, insufficientAuthError(uid))
      res      <- EitherT.cond[Future](assigned == uid, {}, insufficientAuthError(uid))
    } yield res

  private def noTicketFoundError(ticketId: UUID): AppError = TicketNotFound(s"There was no ticket with id `$ticketId` found!")
  private def insufficientAuthError(uid: String): AppError = AuthError(s"User with id `$uid` is not an authorized personnel!")
}

object TicketDetailsService {
  case class MessageDto(
      messageId: UUID,
      createdBy: String,
      createdAt: Long,
      status: MessageStatus,
      text: String,
      reviewedBy: Option[String],
      reviewedAt: Option[Long],
  )

  case class TicketDetailsDto(
      ticketId: UUID,
      createdBy: String,
      createdAt: Long,
      ticketType: TicketType,
      description: String,
      assignedTo: Option[String],
      messages: IndexedSeq[MessageDto],
  )

  def getAnonymText(
      implicit
      lang: Lang,
  ): String = Messages("TopicDetails.anonym")
}
