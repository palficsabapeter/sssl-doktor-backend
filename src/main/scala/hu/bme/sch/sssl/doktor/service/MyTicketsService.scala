package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.TicketType.TicketType
import hu.bme.sch.sssl.doktor.repository.TicketRepository
import hu.bme.sch.sssl.doktor.service.MyTicketsService.MyTicketsResponseDto
import hu.bme.sch.sssl.doktor.util.ErrorUtil.AppErrorOr
import hu.bme.sch.sssl.doktor.util.TicketTransformerUtil.TicketDboTransformer

import scala.concurrent.ExecutionContext

class MyTicketsService(
    implicit
    repo: TicketRepository,
    ec: ExecutionContext,
) {
  def getMyTickets(uid: String): AppErrorOr[MyTicketsResponseDto] =
    for {
      registeredByMe <- EitherT.right(repo.findByUserId(uid).map(_.map(_.toTicketOverviewDto)))
      assignedToMe   <- EitherT.right(repo.findByAssignedUserId(uid).map(_.map(_.toTicketOverviewDto)))
    } yield MyTicketsResponseDto(registeredByMe, assignedToMe)
}

object MyTicketsService {
  case class TicketOverviewDto(
      ticketId: UUID,
      createdAt: Long,
      createdBy: Option[String],
      ticketType: TicketType,
      assignedTo: Option[String],
      isActive: Boolean,
      unansweredMessages: Seq[UUID],
      unreviewedMessages: Seq[UUID],
  )

  case class MyTicketsResponseDto(
      registeredByMe: Seq[TicketOverviewDto],
      assignedToMe: Seq[TicketOverviewDto],
  )
}
