package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.data.{EitherT, OptionT}
import cats.implicits._
import hu.bme.sch.sssl.doktor.repository.{AuthRepository, TicketRepository}
import hu.bme.sch.sssl.doktor.util.ErrorUtil._

import scala.concurrent.{ExecutionContext, Future}

class AssignTicketService(
    implicit
    ticketRepo: TicketRepository,
    authRepo: AuthRepository,
    ec: ExecutionContext,
) {
  def assignTicket(ticketId: UUID, uid: String): AppErrorOr[Unit] =
    for {
      ticket <- OptionT(ticketRepo.findById(ticketId)).toRight(noTicketFoundError(ticketId))
      _      <- OptionT(authRepo.findById(uid)).toRight(insufficientAuthError(uid))
      update <- EitherT.right(ticketRepo.upsert(ticket.copy(assignedTo = Some(uid), isActive = true)))
      res    <- EitherT.cond[Future](update == 1, {}, updateFailed(ticketId))
    } yield res

  def unassignTicket(ticketId: UUID): AppErrorOr[Unit] =
    for {
      ticket <- OptionT(ticketRepo.findById(ticketId)).toRight(noTicketFoundError(ticketId))
      update <- EitherT.right(ticketRepo.upsert(ticket.copy(assignedTo = None)))
      res    <- EitherT.cond[Future](update == 1, {}, updateFailed(ticketId))
    } yield res

  def isAssignedToClerk(ticketId: UUID, clerkId: String): AppErrorOr[Unit] =
    for {
      ticket     <- OptionT(ticketRepo.findById(ticketId)).toRight(noTicketFoundError(ticketId))
      assignedTo <- EitherT.cond[Future](ticket.assignedTo.contains(clerkId), {}, insufficientAuthError(clerkId))
    } yield assignedTo

  private def noTicketFoundError(ticketId: UUID): AppError = TicketNotFound(s"There was no ticket with id `$ticketId` found!")
  private def insufficientAuthError(uid: String): AppError = AuthError(s"User with id `$uid` is not an authorized personnel!")
  private def updateFailed(ticketId: UUID): AppError       = DbActionUnsuccessful(s"Updating ticket with id `$ticketId` failed!")
}
