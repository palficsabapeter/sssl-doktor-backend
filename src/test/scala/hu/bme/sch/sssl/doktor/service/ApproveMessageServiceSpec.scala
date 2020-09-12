package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.MessageStatus
import hu.bme.sch.sssl.doktor.repository.MessageRepository
import hu.bme.sch.sssl.doktor.repository.MessageRepository.MessageDbo
import hu.bme.sch.sssl.doktor.testutil.TestBase
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AppError, MessageNotFound}
import hu.bme.sch.sssl.doktor.util.TimeProvider

class ApproveMessageServiceSpec extends TestBase {
  trait TestScope {
    implicit val repo: MessageRepository = mock[MessageRepository]
    implicit val tp: TimeProvider = new TimeProvider {
      override def epochMillis: Long = 0L
      override def epochSecs: Long   = 0L
    }

    val service: ApproveMessageService = new ApproveMessageService()

    val messageId: UUID = UUID.randomUUID()
    val ticketId: UUID  = UUID.randomUUID()
    val dbo: MessageDbo = MessageDbo(
      messageId,
      ticketId,
      "uid",
      "user",
      0L,
      MessageStatus.Unreviewed,
      "Message text",
      None,
      None,
      None,
    )
  }

  "ApproveMessageService" should {
    "#approveMessage" should {
      "return OK" in new TestScope {
        whenF(repo.findByTicketId(any[UUID])).thenReturn(Seq(dbo))

        await(service.approveMessage(ticketId, messageId, true, "user", "uid").value) shouldBe
          Right({})

        verify(repo).findByTicketId(ticketId)
      }

      "return AppError if there was no messages found for the provided ticketId" in new TestScope {
        private val error: AppError = MessageNotFound(s"Message with id '$messageId' and related ticket id '$ticketId' was not found!")
        whenF(repo.findByTicketId(any[UUID])).thenReturn(Seq.empty[MessageDbo])

        await(service.approveMessage(ticketId, messageId, true, "user", "uid").value) shouldBe
          Left(error)

        verify(repo).findByTicketId(ticketId)
      }

      "return an AppError if there was no messages found for the provided messageId" in new TestScope {
        private val invalidMessageId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        private val error: AppError  = MessageNotFound(s"Message with id '$invalidMessageId' and related ticket id '$ticketId' was not found!")
        whenF(repo.findByTicketId(any[UUID])).thenReturn(Seq(dbo))

        await(service.approveMessage(ticketId, invalidMessageId, true, "user", "uid").value) shouldBe
          Left(error)

        verify(repo).findByTicketId(ticketId)
      }
    }
  }
}
