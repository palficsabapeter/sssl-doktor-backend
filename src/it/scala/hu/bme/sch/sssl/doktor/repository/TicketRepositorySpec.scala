package hu.bme.sch.sssl.doktor.repository

import java.util.UUID

import hu.bme.sch.sssl.doktor.`enum`.TicketType
import hu.bme.sch.sssl.doktor.repository.TicketRepository.TicketDbo
import hu.bme.sch.sssl.doktor.testutil.RepositoryTestBase
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class TicketRepositorySpec extends RepositoryTestBase {
  override def cleanDb(): Future[_] = db.run(sqlu"""TRUNCATE tickets""")

  trait TestScope {
    val repo: TicketRepository = new TicketRepository()

    val ticketId: UUID = UUID.fromString("3debe4ea-14f5-4f7b-8b3b-c81ec2f1ab18")
    val dbo: TicketDbo = TicketDbo(
      ticketId,
      "userId1",
      "User1",
      86400L,
      TicketType.FeedbackRequest,
      false,
      "Description",
      Some("userId11"),
    )
  }

  "TicketRepository" should {
    "#upsert" should {
      "insert new row if there was no matching ticketId" in new TestScope {
        await(for {
          inserted <- repo.upsert(dbo)
          found    <- repo.findById(ticketId)
        } yield {
          inserted shouldBe 1
          found shouldBe a[Some[_]]
          found.get shouldBe dbo.copy(id = found.get.id)
        })
      }

      "update existing row" in new TestScope {
        private val newDbo = dbo.copy(assignedTo = None)

        await(for {
          _       <- repo.upsert(dbo)
          updated <- repo.upsert(newDbo)
          found   <- repo.findById(ticketId)
        } yield {
          updated shouldBe 1
          found shouldBe a[Some[_]]
          found.get shouldBe newDbo.copy(id = found.get.id)
        })
      }
    }

    "#findById" should {
      "return Some(TicketDbo) for found ticketId" in new TestScope {
        await(for {
          _     <- repo.upsert(dbo)
          found <- repo.findById(ticketId)
        } yield {
          found shouldBe a[Some[_]]
          found.get shouldBe dbo.copy(id = found.get.id)
        })
      }

      "return None for not found ticketId" in new TestScope {
        await(repo.findById(ticketId)) shouldBe None
      }
    }

    "#findByUserId" should {
      "return Seq of TicketDbos for uid" in new TestScope {
        private val ticketId2 = UUID.fromString("4f95de93-256d-408c-acda-b19818d7bb3a")
        private val ticketId3 = UUID.fromString("734980f0-8801-44bc-bcd4-7401f4b25c1f")

        await(for {
          _       <- repo.upsert(dbo)
          _       <- repo.upsert(dbo.copy(ticketId = ticketId2))
          _       <- repo.upsert(dbo.copy(uid = "userId11", ticketId = ticketId3))
          tickets <- repo.findByUserId("userId1")
        } yield {
          tickets.size shouldBe 2
          tickets shouldBe Seq(dbo.copy(id = tickets.head.id), dbo.copy(ticketId = ticketId2, id = tickets.tail.head.id))
        })
      }
    }
  }
}
