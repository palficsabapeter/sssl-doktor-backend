package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.TicketType
import hu.bme.sch.sssl.doktor.repository.TicketRepository
import hu.bme.sch.sssl.doktor.repository.TicketRepository.TicketDbo
import hu.bme.sch.sssl.doktor.service.AllTicketsService.AllTicketsResponseDto
import hu.bme.sch.sssl.doktor.service.MyTicketsService.TicketOverviewDto
import hu.bme.sch.sssl.doktor.testutil.TestBase

class AllTicketsServiceSpec extends TestBase {
  trait TestScope {
    implicit val repo: TicketRepository = mock[TicketRepository]

    val service: AllTicketsService = new AllTicketsService()

    val dbos: Seq[TicketDbo] = for {
      i <- 1 to 20
    } yield TicketDbo(
      UUID.fromString("9e779c50-a0a0-40c4-944e-1b3a57132dab"),
      s"userId$i",
      s"User$i",
      0L,
      TicketType.Misc,
      false,
      "Desc",
      None,
      if (i % 2 == 0) true else false,
    )

    val dtos: Seq[TicketOverviewDto] = for {
      i <- 1 to 20
    } yield TicketOverviewDto(
      UUID.fromString("9e779c50-a0a0-40c4-944e-1b3a57132dab"),
      0L,
      Some(s"User$i"),
      TicketType.Misc,
      None,
      if (i % 2 == 0) true else false,
      Seq.empty[UUID],
      Seq.empty[UUID],
    )
  }

  "AllTicketsService" should {
    "#getAllTickets" should {
      "return all tickets" in new TestScope {
        whenF(repo.listAllWithStatusFilter(any[Option[Boolean]])).thenReturn(dbos)

        await(service.getAllTickets(None).value) shouldBe Right(AllTicketsResponseDto(dtos))

        verify(repo, times(1)).listAllWithStatusFilter(None)
      }

      "return only active tickets" in new TestScope {
        whenF(repo.listAllWithStatusFilter(any[Option[Boolean]])).thenReturn(dbos.filter(_.isActive == true))

        await(service.getAllTickets(Some(true)).value) shouldBe
          Right(AllTicketsResponseDto(dtos.filter(_.isActive == true)))

        verify(repo, times(1)).listAllWithStatusFilter(Some(true))
      }

      "return only inactive tickets" in new TestScope {
        whenF(repo.listAllWithStatusFilter(any[Option[Boolean]])).thenReturn(dbos.filter(_.isActive == false))

        await(service.getAllTickets(Some(false)).value) shouldBe
          Right(AllTicketsResponseDto(dtos.filter(_.isActive == false)))

        verify(repo, times(1)).listAllWithStatusFilter(Some(false))
      }
    }
  }
}
