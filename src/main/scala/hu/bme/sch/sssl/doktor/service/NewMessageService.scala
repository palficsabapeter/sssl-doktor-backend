package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.MessageStatus.MessageStatus
import hu.bme.sch.sssl.doktor.repository.{MessageRepository, TicketRepository}
import hu.bme.sch.sssl.doktor.service.NewMessageService.CreateMessageDto
import hu.bme.sch.sssl.doktor.util.ErrorUtil._
import hu.bme.sch.sssl.doktor.util.MessageTransformerUtil._
import hu.bme.sch.sssl.doktor.util.{TimeProvider, UuidProvider}

import scala.concurrent.{ExecutionContext, Future}

class NewMessageService(
    implicit
    ticketRepo: TicketRepository,
    messageRepo: MessageRepository,
    tp: TimeProvider,
    up: UuidProvider,
    ec: ExecutionContext,
) {
  def createMessage(message: CreateMessageDto): AppErrorOr[Unit] = {
    def insertFailed(ticketId: UUID): AppError = DbActionUnsuccessful(s"Inserting message for ticket with id `$ticketId` failed!")
    for {
      _      <- EitherT.fromOptionF(ticketRepo.findById(message.ticketId), noTicketFoundError(message.ticketId))
      insert <- EitherT.right(messageRepo.upsert(message.toMessageDbo))
      res    <- EitherT.cond[Future](insert == 1, {}, insertFailed(message.ticketId))
    } yield res
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

object NewMessageService {
  case class CreateMessageDto(
      ticketId: UUID,
      uid: String,
      createdBy: String,
      status: MessageStatus,
      text: String,
  )
}
