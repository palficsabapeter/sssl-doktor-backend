package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.TicketType
import hu.bme.sch.sssl.doktor.repository.TicketRepository
import hu.bme.sch.sssl.doktor.repository.TicketRepository.TicketDbo
import hu.bme.sch.sssl.doktor.service.MyTicketsService.{MyTicketsResponseDto, TicketOverviewDto}
import hu.bme.sch.sssl.doktor.testutil.TestBase

class MyTicketsServiceSpec extends TestBase {
  trait TestScope {
    implicit val repo: TicketRepository = mock[TicketRepository]

    val service: MyTicketsService = new MyTicketsService()

    val dbo = TicketDbo(
      UUID.fromString("9e779c50-a0a0-40c4-944e-1b3a57132dab"),
      "userId1",
      "User1",
      0L,
      TicketType.Misc,
      false,
      "Desc",
      None,
    )

    val dto = TicketOverviewDto(
      UUID.fromString("9e779c50-a0a0-40c4-944e-1b3a57132dab"),
      0L,
      Some("User1"),
      TicketType.Misc,
      None,
      true,
      Seq.empty[UUID],
      Seq.empty[UUID],
    )

    val dbosRegisteredByMe = Seq(
      dbo,
      dbo.copy(
        ticketId = UUID.fromString("38ec63aa-594c-45e0-840c-2cdd1dac2f64"),
        isAnonym = true,
        assignedTo = Some("userId11"),
      ),
    )

    val dtosRegisteredByMe = Seq(
      dto,
      dto.copy(
        ticketId = UUID.fromString("38ec63aa-594c-45e0-840c-2cdd1dac2f64"),
        createdBy = None,
        assignedTo = Some("userId11"),
      ),
    )

    val dbosAssignedToMe = Seq(
      dbo.copy(
        ticketId = UUID.fromString("9bb42b78-61b6-4132-bb78-3718a6861959"),
        uid = "userId2",
        createdBy = "User2",
        assignedTo = Some("userId1"),
      ),
      dbo.copy(
        ticketId = UUID.fromString("c7fe75dc-2fe8-4756-ba16-f1c0b1048f4d"),
        uid = "userId3",
        createdBy = "User3",
        isAnonym = true,
        assignedTo = Some("userId1"),
      ),
    )

    val dtosAssignedToMe = Seq(
      dto.copy(
        ticketId = UUID.fromString("9bb42b78-61b6-4132-bb78-3718a6861959"),
        createdBy = Some("User2"),
        assignedTo = Some("userId1"),
      ),
      dto.copy(
        ticketId = UUID.fromString("c7fe75dc-2fe8-4756-ba16-f1c0b1048f4d"),
        createdBy = None,
        assignedTo = Some("userId1"),
      ),
    )
  }

  "MyTicketsService" should {
    "#getMyTickets" should {
      "return the tickets registered by me and assigned to me" in new TestScope {
        whenF(repo.findByUserId(any[String])).thenReturn(dbosRegisteredByMe)
        whenF(repo.findByAssignedUserId(any[String])).thenReturn(dbosAssignedToMe)

        await(service.getMyTickets("userId1").value) shouldBe
          Right(MyTicketsResponseDto(dtosRegisteredByMe, dtosAssignedToMe))

        verify(repo, times(1)).findByUserId("userId1")
        verify(repo, times(1)).findByAssignedUserId("userId1")
      }

      "return empty list of tickets registered by me" in new TestScope {
        whenF(repo.findByUserId(any[String])).thenReturn(Seq.empty[TicketDbo])
        whenF(repo.findByAssignedUserId(any[String])).thenReturn(dbosAssignedToMe)

        await(service.getMyTickets("userId1").value) shouldBe
          Right(MyTicketsResponseDto(Seq.empty[TicketOverviewDto], dtosAssignedToMe))

        verify(repo, times(1)).findByUserId("userId1")
        verify(repo, times(1)).findByAssignedUserId("userId1")
      }

      "return empty list of tickets assigned to me" in new TestScope {
        whenF(repo.findByUserId(any[String])).thenReturn(dbosRegisteredByMe)
        whenF(repo.findByAssignedUserId(any[String])).thenReturn(Seq.empty[TicketDbo])

        await(service.getMyTickets("userId1").value) shouldBe
          Right(MyTicketsResponseDto(dtosRegisteredByMe, Seq.empty[TicketOverviewDto]))

        verify(repo, times(1)).findByUserId("userId1")
        verify(repo, times(1)).findByAssignedUserId("userId1")
      }

      "return empty list of tickets both for registered by me and assigned to me" in new TestScope {
        whenF(repo.findByUserId(any[String])).thenReturn(Seq.empty[TicketDbo])
        whenF(repo.findByAssignedUserId(any[String])).thenReturn(Seq.empty[TicketDbo])

        await(service.getMyTickets("userId1").value) shouldBe
          Right(MyTicketsResponseDto(Seq.empty[TicketOverviewDto], Seq.empty[TicketOverviewDto]))

        verify(repo, times(1)).findByUserId("userId1")
        verify(repo, times(1)).findByAssignedUserId("userId1")
      }
    }
  }
}
