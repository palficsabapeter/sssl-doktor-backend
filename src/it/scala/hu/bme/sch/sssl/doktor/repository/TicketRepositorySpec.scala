package hu.bme.sch.sssl.doktor.repository

import java.util.UUID

import hu.bme.sch.sssl.doktor.`enum`.TicketType
import hu.bme.sch.sssl.doktor.repository.TicketRepository.TicketDbo
import hu.bme.sch.sssl.doktor.testutil.RepositoryTestBase
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class TicketRepositorySpec extends RepositoryTestBase {
  override def cleanDb(): Future[_] = db.run(sqlu"""TRUNCATE tickets CASCADE""")

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
          init   <- db.run(repo.tickets.result)
          insert <- repo.upsert(dbo)
          found  <- db.run(repo.tickets.result)
        } yield {
          init.size shouldBe 0
          insert shouldBe 1
          found.size shouldBe 1
          found.head shouldBe dbo.copy(id = found.head.id)
        })
      }

      "update existing row" in new TestScope {
        private val newDbo = dbo.copy(assignedTo = None)

        await(for {
          init          <- db.run(repo.tickets.result)
          insert        <- repo.upsert(dbo)
          insertedFound <- db.run(repo.tickets.result)
          update        <- repo.upsert(newDbo)
          updatedFound  <- db.run(repo.tickets.result)
        } yield {
          init.size shouldBe 0
          insert shouldBe 1
          insertedFound.size shouldBe 1
          insertedFound.head shouldBe dbo.copy(id = insertedFound.head.id)
          update shouldBe 1
          updatedFound.size shouldBe 1
          updatedFound.head shouldBe newDbo.copy(id = updatedFound.head.id)
        })
      }
    }

    "#findById" should {
      "return Some(TicketDbo) for found ticketId" in new TestScope {
        await(for {
          init   <- db.run(repo.tickets.result)
          insert <- repo.upsert(dbo)
          found  <- repo.findById(ticketId)
        } yield {
          init.size shouldBe 0
          insert shouldBe 1
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
          init    <- db.run(repo.tickets.result)
          insert1 <- repo.upsert(dbo)
          insert2 <- repo.upsert(dbo.copy(ticketId = ticketId2))
          insert3 <- repo.upsert(dbo.copy(uid = "userId11", ticketId = ticketId3))
          all     <- db.run(repo.tickets.result)
          tickets <- repo.findByUserId("userId1")
        } yield {
          init.size shouldBe 0
          insert1 shouldBe 1
          insert2 shouldBe 1
          insert3 shouldBe 1
          all.size shouldBe 3
          tickets.size shouldBe 2
          tickets shouldBe Seq(dbo.copy(id = tickets.head.id), dbo.copy(ticketId = ticketId2, id = tickets.tail.head.id))
        })
      }
    }

    "#findByAssignedUserId" should {
      "return Seq of TicketDbos for uid" in new TestScope {
        private val newDbo    = dbo.copy(assignedTo = Some("userId1"))
        private val ticketId2 = UUID.fromString("4f95de93-256d-408c-acda-b19818d7bb3a")
        private val ticketId3 = UUID.fromString("734980f0-8801-44bc-bcd4-7401f4b25c1f")

        await(for {
          init    <- db.run(repo.tickets.result)
          insert1 <- repo.upsert(newDbo)
          insert2 <- repo.upsert(newDbo.copy(ticketId = ticketId2))
          insert3 <- repo.upsert(newDbo.copy(ticketId = ticketId3, assignedTo = None))
          all     <- db.run(repo.tickets.result)
          tickets <- repo.findByAssignedUserId("userId1")
        } yield {
          init.size shouldBe 0
          insert1 shouldBe 1
          insert2 shouldBe 1
          insert3 shouldBe 1
          all.size shouldBe 3
          tickets.size shouldBe 2
          tickets shouldBe Seq(newDbo.copy(id = tickets.head.id), newDbo.copy(ticketId = ticketId2, id = tickets.tail.head.id))
        })
      }
    }

    "#listAllWithStatusFilter" should {
      "list all tickets without filtering" in new TestScope {
        private val newDbo    = dbo.copy(assignedTo = Some("userId1"), isActive = false)
        private val ticketId2 = UUID.fromString("4f95de93-256d-408c-acda-b19818d7bb3a")

        await(for {
          init    <- db.run(repo.tickets.result)
          insert1 <- repo.upsert(newDbo)
          insert2 <- repo.upsert(newDbo.copy(ticketId = ticketId2))
          all     <- db.run(repo.tickets.result)
          tickets <- repo.listAllWithStatusFilter(None)
        } yield {
          init.size shouldBe 0
          insert1 shouldBe 1
          insert2 shouldBe 1
          all.size shouldBe 2
          tickets.size shouldBe 2
          tickets shouldBe Seq(newDbo.copy(id = tickets.head.id), newDbo.copy(ticketId = ticketId2, isActive = false, id = tickets.tail.head.id))
        })
      }

      "list tickets with filtering" in new TestScope {
        private val newDbo    = dbo.copy(assignedTo = Some("userId1"))
        private val ticketId2 = UUID.fromString("4f95de93-256d-408c-acda-b19818d7bb3a")

        await(for {
          init    <- db.run(repo.tickets.result)
          insert1 <- repo.upsert(newDbo)
          insert2 <- repo.upsert(newDbo.copy(ticketId = ticketId2, isActive = false))
          all     <- db.run(repo.tickets.result)
          tickets <- repo.listAllWithStatusFilter(Some(true))
        } yield {
          init.size shouldBe 0
          insert1 shouldBe 1
          insert2 shouldBe 1
          all.size shouldBe 2
          tickets.size shouldBe 1
          tickets.head shouldBe newDbo.copy(id = tickets.head.id)
        })
      }
    }
  }
}
