package hu.bme.sch.sssl.doktor.api

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Route
import cats.implicits._
import hu.bme.sch.sssl.doktor.service.AssignTicketService
import hu.bme.sch.sssl.doktor.testutil.{ApiTestBase, AuthTestUtil}
import hu.bme.sch.sssl.doktor.util.ErrorUtil._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

class AssignTicketApiSpec extends ApiTestBase with AuthTestUtil {
  trait TestScope {
    implicit val service: AssignTicketService = mock[AssignTicketService]

    val api: AssignTicketApi = new AssignTicketApi()
    val route: Route         = api.route

    val ticketId: UUID = UUID.randomUUID()
  }

  "AssignTicketApiSpec" should {
    "POST /tickets/{ticketId}/assign/{userId}" should {
      "return OK" in new TestScope {
        whenF(service.assignTicket(any[UUID], any[String])).thenReturn({})

        Post(s"/tickets/$ticketId/assign/$uid") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(service, times(1)).assignTicket(ticketId, uid)
      }

      "return NotFound if there was no ticket found for the provided ticketId" in new TestScope {
        private val error = TicketNotFound(s"There was no ticket with id `$ticketId` found!")
        whenF(service.assignTicket(any[UUID], any[String])).thenFailWith(error)

        Post(s"/tickets/$ticketId/assign/$uid") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.NotFound
          implicit val decoder: Decoder[TicketNotFound] = deriveDecoder
          responseShouldBeDto[TicketNotFound](error)
        }

        verify(service, times(1)).assignTicket(ticketId, uid)
      }

      "return Unauthorized if there was no sufficient authorities for the provided userId" in new TestScope {
        private val error = AuthError(s"User with id `$uid` is not an authorized personnel!")
        whenF(service.assignTicket(any[UUID], any[String])).thenFailWith(error)

        Post(s"/tickets/$ticketId/assign/$uid") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](error)
        }

        verify(service, times(1)).assignTicket(ticketId, uid)
      }

      "return InternalServerError if updating the ticket failed" in new TestScope {
        private val error = DbActionUnsuccessful(s"Updating ticket with id `$ticketId` failed!")
        whenF(service.assignTicket(any[UUID], any[String])).thenFailWith(error)

        Post(s"/tickets/$ticketId/assign/$uid") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.InternalServerError
          implicit val decoder: Decoder[DbActionUnsuccessful] = deriveDecoder
          responseShouldBeDto[DbActionUnsuccessful](error)
        }

        verify(service, times(1)).assignTicket(ticketId, uid)
      }

      "return Unauthorized if the given credentials did not contain Admin or Clerk authorities" in new TestScope {
        Post(s"/tickets/$ticketId/assign/$uid") ~> addCredentials(OAuth2BearerToken(validTokenWithUserAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("No sufficient authorities!"))
        }

        verify(service, times(0)).assignTicket(any[UUID], any[String])
      }

      "return Unauthorized if the given credentials contained Clerk authorities but the provided userId did not match" in new TestScope {
        Post(s"/tickets/$ticketId/assign/userId2") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("No sufficient authorities!"))
        }

        verify(service, times(0)).assignTicket(any[UUID], any[String])
      }

      "return Unauthorized if there were invalid credentials provided" in new TestScope {
        Post(s"/tickets/$ticketId/assign/$uid") ~> addCredentials(OAuth2BearerToken("in.valid.token")) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("Invalid JWT token!"))
        }

        verify(service, times(0)).assignTicket(any[UUID], any[String])
      }
    }

    "POST /tickets/{ticketId}/unassign" should {
      "return OK" in new TestScope {
        whenF(service.unassignTicket(any[UUID])).thenReturn({})

        Post(s"/tickets/$ticketId/unassign") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(service, times(0)).isAssignedToClerk(any[UUID], any[String])
        verify(service, times(1)).unassignTicket(ticketId)
      }

      "return OK when Clerk unassigns own ticket" in new TestScope {
        whenF(service.isAssignedToClerk(any[UUID], any[String])).thenReturn({})
        whenF(service.unassignTicket(any[UUID])).thenReturn({})

        Post(s"/tickets/$ticketId/unassign") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(service, times(1)).isAssignedToClerk(ticketId, uid)
        verify(service, times(1)).unassignTicket(ticketId)
      }

      "return NotFound if there was no ticket found for the provided ticketId" in new TestScope {
        private val error = TicketNotFound(s"There was no ticket with id `$ticketId` found!")
        whenF(service.isAssignedToClerk(any[UUID], any[String])).thenReturn({})
        whenF(service.unassignTicket(any[UUID])).thenFailWith(error)

        Post(s"/tickets/$ticketId/unassign") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.NotFound
          implicit val decoder: Decoder[TicketNotFound] = deriveDecoder
          responseShouldBeDto[TicketNotFound](error)
        }

        verify(service, times(1)).isAssignedToClerk(ticketId, uid)
        verify(service, times(1)).unassignTicket(ticketId)
      }

      "return InternalServerError if updating ticket failed" in new TestScope {
        private val error = DbActionUnsuccessful(s"Updating ticket with id `$ticketId` failed!")
        whenF(service.isAssignedToClerk(any[UUID], any[String])).thenReturn({})
        whenF(service.unassignTicket(any[UUID])).thenFailWith(error)

        Post(s"/tickets/$ticketId/unassign") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.InternalServerError
          implicit val decoder: Decoder[DbActionUnsuccessful] = deriveDecoder
          responseShouldBeDto[DbActionUnsuccessful](error)
        }

        verify(service, times(1)).isAssignedToClerk(ticketId, uid)
        verify(service, times(1)).unassignTicket(ticketId)
      }

      "return Unauthorized if the given credentials did not contain Admin or Clerk authorities" in new TestScope {
        Post(s"/tickets/$ticketId/unassign") ~> addCredentials(OAuth2BearerToken(validTokenWithUserAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("No sufficient authorities!"))
        }

        verify(service, times(0)).isAssignedToClerk(any[UUID], any[String])
        verify(service, times(0)).unassignTicket(any[UUID])
      }

      "return Unauthorized if the given credentials contained Clerk authorities but the ticket was not assigned to it" in new TestScope {
        private val error = AuthError(s"User with id `$uid` is not an authorized personnel!")
        whenF(service.isAssignedToClerk(any[UUID], any[String])).thenFailWith(error)

        Post(s"/tickets/$ticketId/unassign") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](error)
        }

        verify(service, times(1)).isAssignedToClerk(ticketId, uid)
        verify(service, times(0)).assignTicket(any[UUID], any[String])
      }

      "return Unauthorized if there were invalid credentials provided" in new TestScope {
        Post(s"/tickets/$ticketId/unassign") ~> addCredentials(OAuth2BearerToken("in.valid.token")) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("Invalid JWT token!"))
        }

        verify(service, times(0)).isAssignedToClerk(any[UUID], any[String])
        verify(service, times(0)).unassignTicket(any[UUID])
      }
    }
  }
}
