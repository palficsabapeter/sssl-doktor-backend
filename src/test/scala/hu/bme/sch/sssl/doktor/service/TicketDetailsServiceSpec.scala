package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.{MessageStatus, TicketType}
import hu.bme.sch.sssl.doktor.app.Config
import hu.bme.sch.sssl.doktor.repository.MessageRepository.MessageDbo
import hu.bme.sch.sssl.doktor.repository.TicketRepository.TicketDbo
import hu.bme.sch.sssl.doktor.repository.{MessageRepository, TicketRepository}
import hu.bme.sch.sssl.doktor.service.NewMessageService.CreateMessageDto
import hu.bme.sch.sssl.doktor.service.TicketDetailsService.{MessageDto, TicketDetailsDto}
import hu.bme.sch.sssl.doktor.testutil.TestBase
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AppError, AuthError, DbActionUnsuccessful, TicketNotFound}
import hu.bme.sch.sssl.doktor.util.{TimeProvider, UuidProvider}

class TicketDetailsServiceSpec extends TestBase {
  trait TestScope {
    implicit val config: Config            = new Config {}
    implicit val langConf: Config.LangConf = config.langConf

    implicit val ticketRepo: TicketRepository   = mock[TicketRepository]
    implicit val messageRepo: MessageRepository = mock[MessageRepository]
    implicit val tp: TimeProvider = new TimeProvider {
      override def epochMillis: Long = 0L
      override def epochSecs: Long   = 0L
    }
    implicit val uuidProvider: UuidProvider = new UuidProvider {
      override def generateUuid: UUID = UUID.fromString("4f95de93-256d-408c-acda-b19818d7bb3a")
    }

    val service: TicketDetailsService = new TicketDetailsService()

    val ticketId: UUID = UUID.fromString("52e6d1de-ac56-4cd2-9aa2-2a52ff57cdb5")
    val uid: String    = "userId1"
    val uid2: String   = "userId2"
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

    val message: MessageDbo = MessageDbo(
      uuidProvider.generateUuid,
      ticketId,
      uid,
      "User1",
      0L,
      MessageStatus.Shown,
      "Text",
      Some(uid),
      Some("User2"),
      Some(0L),
    )

    val noTicketError: AppError = TicketNotFound(s"There was no ticket with id `$ticketId` found!")
    val authError: AppError     = AuthError(s"User with id `$uid` is not an authorized personnel!")
    val insertFailed: AppError  = DbActionUnsuccessful(s"Inserting message for ticket with id `$ticketId` failed!")
  }

  "TicketDetailsService" should {
    "#getTicketDetails" should {
      "return a ticket without reviewment infos if it is requested by the ticket creator" in new TestScope {
        private val res = TicketDetailsDto(
          ticketId,
          "User1",
          0L,
          TicketType.AdviceRequest,
          "Desc",
          Some("User2"),
          IndexedSeq(
            MessageDto(
              uuidProvider.generateUuid,
              "User1",
              0L,
              MessageStatus.Shown,
              "Text",
              None,
              None,
            ),
          ),
        )

        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket.copy(assignedTo = Some("User2"))))
        whenF(messageRepo.findByTicketId(any[UUID])).thenReturn(Seq(message))

        await(service.getTicketDetails(ticketId, uid).value) shouldBe Right(res)

        verify(ticketRepo).findById(ticketId)
        verify(messageRepo).findByTicketId(ticketId)
      }

      "return a ticket with createdBy if ticket is anonym and it is requested by the ticket creator" in new TestScope {
        private val res = TicketDetailsDto(
          ticketId,
          "User1",
          0L,
          TicketType.AdviceRequest,
          "Desc",
          Some("User2"),
          IndexedSeq(
            MessageDto(
              uuidProvider.generateUuid,
              "User1",
              0L,
              MessageStatus.Shown,
              "Text",
              None,
              None,
            ),
          ),
        )

        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket.copy(isAnonym = true, assignedTo = Some("User2"))))
        whenF(messageRepo.findByTicketId(any[UUID])).thenReturn(Seq(message))

        await(service.getTicketDetails(ticketId, uid).value) shouldBe Right(res)

        verify(ticketRepo).findById(ticketId)
        verify(messageRepo).findByTicketId(ticketId)
      }

      "return a ticket without createdBy if ticket is anonym and it is requested by other than the ticket creator" in new TestScope {
        private val res = TicketDetailsDto(
          ticketId,
          "Anoním",
          0L,
          TicketType.AdviceRequest,
          "Desc",
          Some("User2"),
          IndexedSeq(
            MessageDto(
              uuidProvider.generateUuid,
              "Anoním",
              0L,
              MessageStatus.Shown,
              "Text",
              Some("User2"),
              Some(0L),
            ),
          ),
        )

        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket.copy(isAnonym = true, assignedTo = Some("User2"))))
        whenF(messageRepo.findByTicketId(any[UUID])).thenReturn(Seq(message))

        await(service.getTicketDetails(ticketId, uid2).value) shouldBe Right(res)

        verify(ticketRepo).findById(ticketId)
        verify(messageRepo).findByTicketId(ticketId)
      }

      "return a ticket with an empy list of messages if it doesn't have any" in new TestScope {
        private val res = TicketDetailsDto(
          ticketId,
          "User1",
          0L,
          TicketType.AdviceRequest,
          "Desc",
          None,
          IndexedSeq.empty,
        )

        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket))
        whenF(messageRepo.findByTicketId(any[UUID])).thenReturn(Seq.empty)

        await(service.getTicketDetails(ticketId, uid).value) shouldBe Right(res)

        verify(ticketRepo).findById(ticketId)
        verify(messageRepo).findByTicketId(ticketId)
      }

      "return an TicketNotFound if ticket was not found for the provided ticketId" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(None)

        await(service.getTicketDetails(ticketId, uid).value) shouldBe Left(noTicketError)

        verify(ticketRepo).findById(ticketId)
        verify(messageRepo, times(0)).findByTicketId(any[UUID])
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
