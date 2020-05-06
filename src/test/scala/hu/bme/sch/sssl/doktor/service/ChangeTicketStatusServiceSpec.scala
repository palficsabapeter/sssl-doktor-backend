package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.TicketType
import hu.bme.sch.sssl.doktor.repository.TicketRepository
import hu.bme.sch.sssl.doktor.repository.TicketRepository.TicketDbo
import hu.bme.sch.sssl.doktor.testutil.TestBase
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{DbActionUnsuccessful, TicketNotFound}

class ChangeTicketStatusServiceSpec extends TestBase {
  trait TestScope {
    implicit val repo: TicketRepository = mock[TicketRepository]

    val service: ChangeTicketStatusService = new ChangeTicketStatusService()

    val dbo = TicketDbo(
      UUID.randomUUID(),
      "userId1",
      "User1",
      0L,
      TicketType.Misc,
      false,
      "Desc",
      None,
    )
  }

  "ChangeTicketStatusService" should {
    "#changeTicketStatus" should {
      "open ticket" in new TestScope {
        whenF(repo.findById(any[UUID])).thenReturn(Some(dbo))
        whenF(repo.upsert(any[TicketDbo])).thenReturn(1)

        await(service.changeTicketStatus(dbo.ticketId, true).value) shouldBe Right({})

        verify(repo, times(1)).findById(dbo.ticketId)
        verify(repo, times(1)).upsert(dbo.copy(isActive = true))
      }

      "close ticket" in new TestScope {
        whenF(repo.findById(any[UUID])).thenReturn(Some(dbo))
        whenF(repo.upsert(any[TicketDbo])).thenReturn(1)

        await(service.changeTicketStatus(dbo.ticketId, false).value) shouldBe Right({})

        verify(repo, times(1)).findById(dbo.ticketId)
        verify(repo, times(1)).upsert(dbo.copy(isActive = false))
      }

      "return a TicketNotFound if there was no ticket was found for provided id" in new TestScope {
        whenF(repo.findById(any[UUID])).thenReturn(None)

        await(service.changeTicketStatus(dbo.ticketId, false).value) shouldBe
          Left(TicketNotFound(s"There was no ticket with id `${dbo.ticketId}` found!"))

        verify(repo, times(1)).findById(dbo.ticketId)
        verify(repo, times(0)).upsert(any[TicketDbo])
      }

      "return a DbActionUnsuccessful if updating ticket failed" in new TestScope {
        whenF(repo.findById(any[UUID])).thenReturn(Some(dbo))
        whenF(repo.upsert(any[TicketDbo])).thenReturn(0)

        await(service.changeTicketStatus(dbo.ticketId, false).value) shouldBe
          Left(DbActionUnsuccessful(s"Updating ticket with id `${dbo.ticketId}` failed!"))

        verify(repo, times(1)).findById(dbo.ticketId)
        verify(repo, times(1)).upsert(dbo.copy(isActive = false))
      }
    }
  }
}
