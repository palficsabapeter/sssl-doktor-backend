package hu.bme.sch.sssl.doktor.api

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Route
import cats.implicits._
import hu.bme.sch.sssl.doktor.service.ChangeTicketStatusService
import hu.bme.sch.sssl.doktor.testutil.{ApiTestBase, AuthTestUtil}
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AuthError, DbActionUnsuccessful, TicketNotFound}

class TicketApiSpec extends ApiTestBase with AuthTestUtil {
  import io.circe.Decoder
  import io.circe.generic.semiauto._

  trait TestScope {
    implicit val service: ChangeTicketStatusService = mock[ChangeTicketStatusService]

    val api: TicketApi = new TicketApi()

    val route: Route = api.route
  }

  "TicketApi" should {
    "POST /ticket/{ticketId}/open" should {
      "return OK" in new TestScope {
        whenF(service.changeTicketStatus(any[UUID], any[Boolean])).thenReturn({})

        private val uuid = UUID.randomUUID()
        Post(s"/tickets/$uuid/open") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.OK
        }

        verify(service, times(1)).changeTicketStatus(uuid, true)
      }

      "return NotFound if there was no ticket found for the provided id" in new TestScope {
        private val uuid  = UUID.randomUUID()
        private val error = TicketNotFound(s"There was no ticket with id `$uuid` found!")
        whenF(service.changeTicketStatus(any[UUID], any[Boolean])).thenFailWith(error)

        Post(s"/tickets/$uuid/open") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.NotFound
          implicit val decoder: Decoder[TicketNotFound] = deriveDecoder
          responseShouldBeDto[TicketNotFound](error)
        }

        verify(service, times(1)).changeTicketStatus(uuid, true)
      }

      "return InternalServerError if updating the ticket failed" in new TestScope {
        private val uuid  = UUID.randomUUID()
        private val error = DbActionUnsuccessful(s"Updating ticket with id `$uuid` failed!")
        whenF(service.changeTicketStatus(any[UUID], any[Boolean])).thenFailWith(error)

        Post(s"/tickets/$uuid/open") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.InternalServerError
          implicit val decoder: Decoder[DbActionUnsuccessful] = deriveDecoder
          responseShouldBeDto[DbActionUnsuccessful](error)
        }

        verify(service, times(1)).changeTicketStatus(uuid, true)
      }

      "return Unauthorized if the given credentials did not contain Clerk authorities" in new TestScope {
        Post(s"/tickets/${UUID.randomUUID()}/open") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("No sufficient authorities!"))
        }

        verify(service, times(0)).changeTicketStatus(any[UUID], any[Boolean])
      }

      "return Unauthorized if there were no credentials provided" in new TestScope {
        Post(s"/tickets/${UUID.randomUUID()}/open") ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("The user is not authenticated!"))
        }

        verify(service, times(0)).changeTicketStatus(any[UUID], any[Boolean])
      }

      "return Unauthorized if there were invalid credentials provided" in new TestScope {
        Post(s"/tickets/${UUID.randomUUID()}/open") ~> addCredentials(OAuth2BearerToken("in.valid.token")) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("Invalid JWT token!"))
        }

        verify(service, times(0)).changeTicketStatus(any[UUID], any[Boolean])
      }
    }

    "POST /ticket/{ticketId}/close" should {
      "return OK" in new TestScope {
        whenF(service.changeTicketStatus(any[UUID], any[Boolean])).thenReturn({})

        private val uuid = UUID.randomUUID()
        Post(s"/tickets/$uuid/close") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.OK
        }

        verify(service, times(1)).changeTicketStatus(uuid, false)
      }

      "return NotFound if there was no ticket found for the provided id" in new TestScope {
        private val uuid  = UUID.randomUUID()
        private val error = TicketNotFound(s"There was no ticket with id `$uuid` found!")
        whenF(service.changeTicketStatus(any[UUID], any[Boolean])).thenFailWith(error)

        Post(s"/tickets/$uuid/close") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.NotFound
          implicit val decoder: Decoder[TicketNotFound] = deriveDecoder
          responseShouldBeDto[TicketNotFound](error)
        }

        verify(service, times(1)).changeTicketStatus(uuid, false)
      }

      "return InternalServerError if updating the ticket failed" in new TestScope {
        private val uuid  = UUID.randomUUID()
        private val error = DbActionUnsuccessful(s"Updating ticket with id `$uuid` failed!")
        whenF(service.changeTicketStatus(any[UUID], any[Boolean])).thenFailWith(error)

        Post(s"/tickets/$uuid/close") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.InternalServerError
          implicit val decoder: Decoder[DbActionUnsuccessful] = deriveDecoder
          responseShouldBeDto[DbActionUnsuccessful](error)
        }

        verify(service, times(1)).changeTicketStatus(uuid, false)
      }

      "return Unauthorized if the given credentials did not contain Clerk authorities" in new TestScope {
        Post(s"/tickets/${UUID.randomUUID()}/close") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("No sufficient authorities!"))
        }

        verify(service, times(0)).changeTicketStatus(any[UUID], any[Boolean])
      }

      "return Unauthorized if there were no credentials provided" in new TestScope {
        Post(s"/tickets/${UUID.randomUUID()}/close") ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("The user is not authenticated!"))
        }

        verify(service, times(0)).changeTicketStatus(any[UUID], any[Boolean])
      }

      "return Unauthorized if there were invalid credentials provided" in new TestScope {
        Post(s"/tickets/${UUID.randomUUID()}/close") ~> addCredentials(OAuth2BearerToken("in.valid.token")) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("Invalid JWT token!"))
        }

        verify(service, times(0)).changeTicketStatus(any[UUID], any[Boolean])
      }
    }
  }
}
