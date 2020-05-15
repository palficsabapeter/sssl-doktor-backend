package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.{Authorities, TicketType}
import hu.bme.sch.sssl.doktor.repository.AuthRepository.UserAuthDbo
import hu.bme.sch.sssl.doktor.repository.TicketRepository.TicketDbo
import hu.bme.sch.sssl.doktor.repository.{AuthRepository, TicketRepository}
import hu.bme.sch.sssl.doktor.testutil.TestBase
import hu.bme.sch.sssl.doktor.util.ErrorUtil._

class AssignTicketServiceSpec extends TestBase {
  trait TestScope {
    implicit val authRepo: AuthRepository     = mock[AuthRepository]
    implicit val ticketRepo: TicketRepository = mock[TicketRepository]

    val service: AssignTicketService = new AssignTicketService()

    val uid: String       = "userId1"
    val auth: UserAuthDbo = UserAuthDbo(uid, Seq(Authorities.Admin, Authorities.Clerk, Authorities.User))

    val ticketId: UUID = UUID.fromString("b8ec6eaa-29ab-469d-b481-5435c8f4f7e8")
    val ticket: TicketDbo = TicketDbo(
      ticketId,
      "userId2",
      "User2",
      0L,
      TicketType.AdviceRequest,
      false,
      "desc",
      None,
      false,
    )

    val assignedTicket: TicketDbo = ticket.copy(assignedTo = Some(uid))
  }

  "AssignTicketService" should {
    "#assignTicket" should {
      "assign ticket" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket))
        whenF(authRepo.findById(any[String])).thenReturn(Some(auth))
        whenF(ticketRepo.upsert(any[TicketDbo])).thenReturn(1)

        await(service.assignTicket(ticketId, uid).value) shouldBe Right({})

        verify(ticketRepo, times(1)).findById(ticketId)
        verify(authRepo, times(1)).findById(uid)
        verify(ticketRepo, times(1)).upsert(ticket.copy(assignedTo = Some(uid), isActive = true))
      }

      "return a TicketNotFound if there was not ticket found for provided ticketId" in new TestScope {
        private val error = TicketNotFound(s"There was no ticket with id `$ticketId` found!")
        whenF(ticketRepo.findById(any[UUID])).thenReturn(None)

        await(service.assignTicket(ticketId, uid).value) shouldBe Left(error)

        verify(ticketRepo, times(1)).findById(ticketId)
        verify(authRepo, times(0)).findById(any[String])
        verify(ticketRepo, times(0)).upsert(any[TicketDbo])
      }

      "return an AuthError if there was no sufficient authorization found for provided uid" in new TestScope {
        private val error = AuthError(s"User with id `$uid` is not an authorized personnel!")
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket))
        whenF(authRepo.findById(any[String])).thenReturn(None)

        await(service.assignTicket(ticketId, uid).value) shouldBe Left(error)

        verify(ticketRepo, times(1)).findById(ticketId)
        verify(authRepo, times(1)).findById(uid)
        verify(ticketRepo, times(0)).upsert(any[TicketDbo])
      }

      "return a DbActionUnsuccessful if updating ticket failed" in new TestScope {
        private val error = DbActionUnsuccessful(s"Updating ticket with id `$ticketId` failed!")
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(ticket))
        whenF(authRepo.findById(any[String])).thenReturn(Some(auth))
        whenF(ticketRepo.upsert(any[TicketDbo])).thenReturn(0)

        await(service.assignTicket(ticketId, uid).value) shouldBe Left(error)

        verify(ticketRepo, times(1)).findById(ticketId)
        verify(authRepo, times(1)).findById(uid)
        verify(ticketRepo, times(1)).upsert(ticket.copy(assignedTo = Some(uid), isActive = true))
      }
    }

    "#unassignTicket" should {
      "unassign ticket" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(assignedTicket))
        whenF(ticketRepo.upsert(any[TicketDbo])).thenReturn(1)

        await(service.unassignTicket(ticketId).value) shouldBe Right({})

        verify(ticketRepo, times(1)).findById(ticketId)
        verify(ticketRepo, times(1)).upsert(assignedTicket.copy(assignedTo = None))
      }

      "return a TicketNotFound if there was not ticket found for provided ticketId" in new TestScope {
        private val error = TicketNotFound(s"There was no ticket with id `$ticketId` found!")
        whenF(ticketRepo.findById(any[UUID])).thenReturn(None)

        await(service.unassignTicket(ticketId).value) shouldBe Left(error)

        verify(ticketRepo, times(1)).findById(ticketId)
        verify(ticketRepo, times(0)).upsert(any[TicketDbo])
      }

      "return a DbActionUnsuccessful if updating ticket failed" in new TestScope {
        private val error = DbActionUnsuccessful(s"Updating ticket with id `$ticketId` failed!")
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(assignedTicket))
        whenF(ticketRepo.upsert(any[TicketDbo])).thenReturn(0)

        await(service.unassignTicket(ticketId).value) shouldBe Left(error)

        verify(ticketRepo, times(1)).findById(ticketId)
        verify(ticketRepo, times(1)).upsert(assignedTicket.copy(assignedTo = None))
      }
    }

    "#isAssignedToClerk" should {
      "return a Unit if ticket is assigned to provided uid" in new TestScope {
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(assignedTicket))

        await(service.isAssignedToClerk(ticketId, uid).value) shouldBe Right({})

        verify(ticketRepo, times(1)).findById(ticketId)
      }

      "return a TicketNotFound if there was not ticket found for provided ticketId" in new TestScope {
        private val error = TicketNotFound(s"There was no ticket with id `$ticketId` found!")
        whenF(ticketRepo.findById(any[UUID])).thenReturn(None)

        await(service.isAssignedToClerk(ticketId, uid).value) shouldBe Left(error)

        verify(ticketRepo, times(1)).findById(ticketId)
      }

      "return an AuthError if there was no sufficient authorization found for provided uid" in new TestScope {
        private val error = AuthError("User with id `userId2` is not an authorized personnel!")
        whenF(ticketRepo.findById(any[UUID])).thenReturn(Some(assignedTicket))

        await(service.isAssignedToClerk(ticketId, "userId2").value) shouldBe Left(error)

        verify(ticketRepo, times(1)).findById(ticketId)
      }
    }
  }
}
