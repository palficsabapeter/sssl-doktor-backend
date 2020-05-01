package hu.bme.sch.sssl.doktor.service

import cats.data.EitherT
import hu.bme.sch.sssl.doktor.api.TicketsApi.CreateTicketDto
import hu.bme.sch.sssl.doktor.repository.TicketRepository
import hu.bme.sch.sssl.doktor.repository.TicketRepository.TicketDbo
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AppErrorOr, DbActionUnsuccessful}
import hu.bme.sch.sssl.doktor.util.{TimeProvider, UuidProvider}

import scala.concurrent.ExecutionContext

class NewTicketService(
    implicit
    repo: TicketRepository,
    tp: TimeProvider,
    up: UuidProvider,
    ec: ExecutionContext,
) {
  def createTicket(uid: String, createdBy: String, dto: CreateTicketDto): AppErrorOr[Unit] = {
    val dbo = TicketDbo(
      up.generateUuid,
      uid,
      createdBy,
      tp.epochMillis,
      dto.ticketType,
      dto.isAnonym,
      dto.description,
      None,
    )

    val insert = repo.upsert(dbo).map {
      case 1 => Right({})
      case _ => Left(DbActionUnsuccessful("Could not insert ticket into database!"))
    }

    EitherT(insert)
  }
}
