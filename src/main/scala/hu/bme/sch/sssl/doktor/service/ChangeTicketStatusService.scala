package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.data.{EitherT, OptionT}
import cats.implicits._
import hu.bme.sch.sssl.doktor.repository.TicketRepository
import hu.bme.sch.sssl.doktor.util.ErrorUtil._

import scala.concurrent.{ExecutionContext, Future}

class ChangeTicketStatusService(
    implicit
    repo: TicketRepository,
    ec: ExecutionContext,
) {
  def changeTicketStatus(ticketId: UUID, shouldBeActive: Boolean): AppErrorOr[Unit] = {
    val noTicketFoundError: AppError = TicketNotFound(s"There was no ticket with id `$ticketId` found!")
    val updateFailed: AppError       = DbActionUnsuccessful(s"Updating ticket with id `$ticketId` failed!")
    for {
      ticket <- OptionT(repo.findById(ticketId)).toRight(noTicketFoundError)
      upsert <- EitherT.right(repo.upsert(ticket.copy(isActive = shouldBeActive)))
      res    <- EitherT.cond[Future](upsert == 1, {}, updateFailed)
    } yield res
  }
}
