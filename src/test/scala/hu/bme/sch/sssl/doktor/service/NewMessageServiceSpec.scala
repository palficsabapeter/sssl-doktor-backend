package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.{MessageStatus, TicketType}
import hu.bme.sch.sssl.doktor.repository.MessageRepository.MessageDbo
import hu.bme.sch.sssl.doktor.repository.TicketRepository.TicketDbo
import hu.bme.sch.sssl.doktor.repository.{MessageRepository, TicketRepository}
import hu.bme.sch.sssl.doktor.service.NewMessageService.CreateMessageDto
import hu.bme.sch.sssl.doktor.testutil.TestBase
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AppError, AuthError, DbActionUnsuccessful, TicketNotFound}
import hu.bme.sch.sssl.doktor.util.{TimeProvider, UuidProvider}

class NewMessageServiceSpec extends TestBase {
  trait TestScope {
    implicit val ticketRepo: TicketRepository   = mock[TicketRepository]
    implicit val messageRepo: MessageRepository = mock[MessageRepository]
    implicit val tp: TimeProvider = new TimeProvider {
      override def epochMillis: Long = 0L
      override def epochSecs: Long   = 0L
    }
    implicit val uuidProvider: UuidProvider = new UuidProvider {
      override def generateUuid: UUID = UUID.fromString("4f95de93-256d-408c-acda-b19818d7bb3a")
    }

    val service: NewMessageService = new NewMessageService()

    val ticketId: UUID = UUID.fromString("52e6d1de-ac56-4cd2-9aa2-2a52ff57cdb5")
    val uid: String    = "userId1"
    val ticket: TicketDbo = TicketDbo(
      ticketId,
      uid,
      "User1",
      0L,
      TicketType.AdviceRequest,
      false,
      "Desc",
      None,
    )

    val dto: CreateMessageDto = CreateMessageDto(
      ticketId,
      uid,
      "User1",
      MessageStatus.Shown,
      "Text",
    )

    val dbo: MessageDbo = MessageDbo(
      uuidProvider.generateUuid,
      ticketId,
      uid,
      "User1",
      0L,
      MessageStatus.Shown,
      "Text",
      Some(uid),
      Some("User1"),
      Some(0L),
    )

    val noTicketError: AppError = TicketNotFound(s"There was no ticket with id `$ticketId` found!")
    val authError: AppError     = AuthError(s"User with id `$uid` is not an authorized personnel!")
    val insertFailed: AppError  = DbActionUnsuccessful(s"Inserting message for ticket with id `$ticketId` failed!")
  }

  "NewMessageService" should {
    "#createMessage" should {
      "return a Unit if ticket creation was successful" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket))
        whenF(messageRepo.upsert(any[MessageDbo])).thenReturn(1)

        await(service.createMessage(dto).value) shouldBe Right({})

        verify(ticketRepo, times(1)).findById(ticketId)
        verify(messageRepo, times(1)).upsert(dbo)
      }

      "return a TicketNotFound if there was no ticket found for the provided ticketId" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(None)

        await(service.createMessage(dto).value) shouldBe Left(noTicketError)

        verify(ticketRepo, times(1)).findById(ticketId)
        verify(messageRepo, times(0)).upsert(any[MessageDbo])
      }

      "return a DbActionUnsuccessful if inserting message failed" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket))
        whenF(messageRepo.upsert(any[MessageDbo])).thenReturn(0)

        await(service.createMessage(dto).value) shouldBe Left(insertFailed)

        verify(ticketRepo, times(1)).findById(ticketId)
        verify(messageRepo, times(1)).upsert(dbo)
      }
    }

    "#isCreatedByUid" should {
      "return a Unit if ticket is created by the provided uid" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket))

        await(service.isCreatedByUid(ticketId, uid).value) shouldBe Right({})

        verify(ticketRepo, times(1)).findById(ticketId)
      }

      "return a TicketNotFound if there was no ticket found for the provided ticketId" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(None)

        await(service.isCreatedByUid(ticketId, uid).value) shouldBe Left(noTicketError)

        verify(ticketRepo, times(1)).findById(ticketId)
      }

      "return an AuthError if the ticket did not belong to the provided uid" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket.copy(uid = "userId2")))

        await(service.isCreatedByUid(ticketId, uid).value) shouldBe Left(authError)

        verify(ticketRepo, times(1)).findById(ticketId)
      }
    }

    "#isAssignedToUid" should {
      "return a Unit if ticket is assigned to the provided uid" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket.copy(assignedTo = Some(uid))))

        await(service.isAssignedToUid(ticketId, uid).value) shouldBe Right({})

        verify(ticketRepo, times(1)).findById(ticketId)
      }

      "return a TicketNotFound if there was no ticket found for the provided ticketId" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(None)

        await(service.isAssignedToUid(ticketId, uid).value) shouldBe Left(noTicketError)

        verify(ticketRepo, times(1)).findById(ticketId)
      }

      "return an AuthError if the ticket is not assigned to anybody" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket))

        await(service.isAssignedToUid(ticketId, uid).value) shouldBe Left(authError)

        verify(ticketRepo, times(1)).findById(ticketId)
      }

      "return an AuthError if the ticket is not assigned to the provided uid" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket.copy(assignedTo = Some("userId2"))))

        await(service.isAssignedToUid(ticketId, uid).value) shouldBe Left(authError)

        verify(ticketRepo, times(1)).findById(ticketId)
      }
    }
  }
}
