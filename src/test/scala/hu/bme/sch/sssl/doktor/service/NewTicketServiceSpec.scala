package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.TicketType
import hu.bme.sch.sssl.doktor.api.TicketsApi.CreateTicketDto
import hu.bme.sch.sssl.doktor.repository.TicketRepository
import hu.bme.sch.sssl.doktor.repository.TicketRepository.TicketDbo
import hu.bme.sch.sssl.doktor.testutil.TestBase
import hu.bme.sch.sssl.doktor.util.ErrorUtil.DbActionUnsuccessful
import hu.bme.sch.sssl.doktor.util.{TimeProvider, UuidProvider}

class NewTicketServiceSpec extends TestBase {
  trait TestScope {
    implicit val repo: TicketRepository = mock[TicketRepository]
    implicit val tp: TimeProvider = new TimeProvider {
      override def epochMillis: Long = 0L
      override def epochSecs: Long   = 0L
    }
    implicit val uuidProvider: UuidProvider = new UuidProvider {
      override def generateUuid: UUID = UUID.fromString("4f95de93-256d-408c-acda-b19818d7bb3a")
    }

    val service: NewTicketService = new NewTicketService()

    val dto: CreateTicketDto = CreateTicketDto(
      TicketType.AdviceRequest,
      true,
      "Description",
    )

    val dbo: TicketDbo = TicketDbo(
      uuidProvider.generateUuid,
      "userId1",
      "User1",
      tp.epochMillis,
      TicketType.AdviceRequest,
      true,
      "Description",
      None,
    )
  }

  "NewTicketService" should {
    "#createTicket" should {
      "return a Unit if ticket creation was successful" in new TestScope {
        whenF(repo.upsert(any[TicketDbo])).thenReturn(1)

        await(service.createTicket("userId1", "User1", dto).value) shouldBe Right({})

        verify(repo, times(1)).upsert(dbo)
      }

      "return an DbActionUnsuccessful if ticket creation failed" in new TestScope {
        whenF(repo.upsert(any[TicketDbo])).thenReturn(0)

        await(service.createTicket("userId1", "User1", dto).value) shouldBe
          Left(DbActionUnsuccessful("Could not insert ticket into database!"))

        verify(repo, times(1)).upsert(dbo)
      }
    }
  }
}
