package hu.bme.sch.sssl.doktor.service

import cats.data.EitherT
import cats.implicits._
import hu.bme.sch.sssl.doktor.repository.TicketRepository
import hu.bme.sch.sssl.doktor.service.AllTicketsService.AllTicketsResponseDto
import hu.bme.sch.sssl.doktor.service.MyTicketsService.TicketOverviewDto
import hu.bme.sch.sssl.doktor.util.ErrorUtil.AppErrorOr
import hu.bme.sch.sssl.doktor.util.TicketTransformerUtil._

import scala.concurrent.ExecutionContext

class AllTicketsService(
    implicit
    repo: TicketRepository,
    ec: ExecutionContext,
) {
  def getAllTickets(status: Option[Boolean]): AppErrorOr[AllTicketsResponseDto] =
    EitherT
      .right(
        repo
          .listAllWithStatusFilter(status)
          .map(dbos => AllTicketsResponseDto(dbos.toList.map(_.toTicketOverviewDto))),
      )
}

object AllTicketsService {
  case class AllTicketsResponseDto(
      tickets: Seq[TicketOverviewDto],
  )
}
