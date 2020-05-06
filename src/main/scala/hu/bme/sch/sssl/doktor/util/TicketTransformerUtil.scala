package hu.bme.sch.sssl.doktor.util

import java.util.UUID

import hu.bme.sch.sssl.doktor.repository.TicketRepository.TicketDbo
import hu.bme.sch.sssl.doktor.service.MyTicketsService.TicketOverviewDto

object TicketTransformerUtil {
  implicit class TicketDboTransformer(dbo: TicketDbo) {
    def toTicketOverviewDto: TicketOverviewDto = {
      val createdBy =
        if (dbo.isAnonym)
          None
        else
          Some(dbo.createdBy)

      TicketOverviewDto(
        dbo.ticketId,
        dbo.createdAt,
        createdBy,
        dbo.ticketType,
        dbo.assignedTo,
        dbo.isActive,
        Seq.empty[UUID],
        Seq.empty[UUID],
      )
    }
  }
}
