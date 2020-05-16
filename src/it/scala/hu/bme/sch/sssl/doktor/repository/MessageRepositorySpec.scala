package hu.bme.sch.sssl.doktor.repository

import java.util.UUID

import hu.bme.sch.sssl.doktor.`enum`.{MessageStatus, TicketType}
import hu.bme.sch.sssl.doktor.repository.MessageRepository.MessageDbo
import hu.bme.sch.sssl.doktor.repository.TicketRepository.TicketDbo
import hu.bme.sch.sssl.doktor.testutil.RepositoryTestBase
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class MessageRepositorySpec extends RepositoryTestBase {
  override def cleanDb(): Future[_] = db.run(sqlu"""TRUNCATE messages; TRUNCATE tickets CASCADE;""")

  trait TestScope {
    val ticketRepo: TicketRepository   = new TicketRepository()
    val messageRepo: MessageRepository = new MessageRepository()

    val ticketId: UUID = UUID.fromString("3debe4ea-14f5-4f7b-8b3b-c81ec2f1ab18")
    val ticket: TicketDbo = TicketDbo(
      ticketId,
      "userId1",
      "User1",
      86400L,
      TicketType.FeedbackRequest,
      false,
      "Description",
      Some("userId11"),
    )
    val ticketId2: UUID    = UUID.fromString("82b43602-a2a4-4fc7-a3fe-87b06ef96cfd")
    val ticket2: TicketDbo = ticket.copy(ticketId = ticketId2)

    val messageId: UUID = UUID.fromString("666fc74b-23cf-4657-950d-e93c1e994569")
    val message: MessageDbo = MessageDbo(
      messageId,
      ticketId,
      "userId1",
      "User1",
      90000L,
      MessageStatus.Shown,
      "Text",
      Some("userId1"),
      Some("User1"),
      Some(90000L),
    )
    val messageId2: UUID = UUID.fromString("efe081b2-c22d-4834-85c4-e4451dc7c0ac")
    val message2: MessageDbo = MessageDbo(
      messageId2,
      ticketId,
      "userId11",
      "User11",
      11000000L,
      MessageStatus.Unreviewed,
      "Text",
      None,
      None,
      None,
    )

    val messageId3: UUID = UUID.fromString("48994579-3dc7-4765-aa8f-3195f312db52")
    val message3: MessageDbo = MessageDbo(
      messageId3,
      ticketId2,
      "userId1",
      "User1",
      256000L,
      MessageStatus.Shown,
      "Text",
      Some("userId1"),
      Some("User1"),
      Some(256000L),
    )
  }

  "MessageRepository" should {
    "#upsert" should {
      "insert new row" in new TestScope {
        await(for {
          _      <- ticketRepo.upsert(ticket)
          init   <- db.run(messageRepo.messages.result)
          insert <- messageRepo.upsert(message)
          found  <- messageRepo.findById(messageId)
        } yield {
          init.size shouldBe 0
          insert shouldBe 1
          found shouldBe a[Some[_]]
          found.get shouldBe message.copy(id = found.get.id)
        })
      }

      "update existing row" in new TestScope {
        private val newMessage = message2.copy(reviewedByUid = Some("userId20"), reviewedBy = Some("User20"), reviewedAt = Some(1100000000L))
        await(for {
          _             <- ticketRepo.upsert(ticket)
          init          <- db.run(messageRepo.messages.result)
          insert        <- messageRepo.upsert(message2)
          insertedFound <- db.run(messageRepo.messages.result)
          update        <- messageRepo.upsert(newMessage)
          updatedFound  <- db.run(messageRepo.messages.result)
        } yield {
          init.size shouldBe 0
          insert shouldBe 1
          insertedFound.size shouldBe 1
          insertedFound.head shouldBe message2.copy(id = insertedFound.head.id)
          update shouldBe 1
          updatedFound.size shouldBe 1
          updatedFound.head shouldBe newMessage.copy(id = updatedFound.head.id)
        })
      }
    }

    "#findById" should {
      "return Some(MessageDbo) for found messageId" in new TestScope {
        await(for {
          _       <- ticketRepo.upsert(ticket)
          init    <- db.run(messageRepo.messages.result)
          insert  <- messageRepo.upsert(message)
          insert2 <- messageRepo.upsert(message2)
          found   <- messageRepo.findById(messageId)
        } yield {
          init.size shouldBe 0
          insert shouldBe 1
          insert2 shouldBe 1
          found shouldBe a[Some[_]]
          found.get shouldBe message.copy(id = found.get.id)
        })
      }

      "return None for not found messageId" in new TestScope {
        await(messageRepo.findById(messageId)) shouldBe None
      }
    }

    "#findByTicketId" should {
      "return Seq of MessageDbos for ticketId" in new TestScope {
        await(for {
          _       <- ticketRepo.upsert(ticket)
          _       <- ticketRepo.upsert(ticket2)
          init    <- db.run(messageRepo.messages.result)
          insert  <- messageRepo.upsert(message)
          insert2 <- messageRepo.upsert(message2)
          insert3 <- messageRepo.upsert(message3)
          found   <- messageRepo.findByTicketId(ticketId)
        } yield {
          init.size shouldBe 0
          insert shouldBe 1
          insert2 shouldBe 1
          insert3 shouldBe 1
          found.size shouldBe 2
          found.head shouldBe message.copy(id = found.head.id)
          found.last shouldBe message2.copy(id = found.last.id)
        })
      }

      "return an empty list if there was no MessageDbos found for the provided ticketId" in new TestScope {
        await(for {
          _       <- ticketRepo.upsert(ticket)
          _       <- ticketRepo.upsert(ticket2)
          init    <- db.run(messageRepo.messages.result)
          insert  <- messageRepo.upsert(message)
          insert2 <- messageRepo.upsert(message2)
          found   <- messageRepo.findByTicketId(ticketId2)
        } yield {
          init.size shouldBe 0
          insert shouldBe 1
          insert2 shouldBe 1
          found shouldBe Nil
        })
      }
    }

    "#listAllWithStatusFilter" should {
      "return Seq of MessageDbos that match the filter" in new TestScope {
        await(for {
          _       <- ticketRepo.upsert(ticket)
          _       <- ticketRepo.upsert(ticket2)
          init    <- db.run(messageRepo.messages.result)
          insert  <- messageRepo.upsert(message)
          insert2 <- messageRepo.upsert(message2)
          insert3 <- messageRepo.upsert(message3)
          found   <- messageRepo.listAllWithStatusFilter(MessageStatus.Shown)
        } yield {
          init.size shouldBe 0
          insert shouldBe 1
          insert2 shouldBe 1
          insert3 shouldBe 1
          found.size shouldBe 2
          found.head shouldBe message.copy(id = found.head.id)
          found.last shouldBe message3.copy(id = found.last.id)
        })
      }
    }
  }
}
