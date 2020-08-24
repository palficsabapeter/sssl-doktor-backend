package hu.bme.sch.sssl.doktor.api

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Route
import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.TicketType
import hu.bme.sch.sssl.doktor.service.TicketDetailsService.{MessageDto, TicketDetailsDto}
import hu.bme.sch.sssl.doktor.service.{ChangeTicketStatusService, TicketDetailsService}
import hu.bme.sch.sssl.doktor.testutil.{ApiTestBase, AuthTestUtil}
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AuthError, DbActionUnsuccessful, TicketNotFound}
import hu.bme.sch.sssl.doktor.util.TimeProvider

class TicketApiSpec extends ApiTestBase with AuthTestUtil {
  import io.circe.Decoder
  import io.circe.generic.semiauto._

  trait TestScope {
    implicit val changeTicketStatusService: ChangeTicketStatusService = mock[ChangeTicketStatusService]
    implicit val ticketDetailsService: TicketDetailsService           = mock[TicketDetailsService]

    val api: TicketApi = new TicketApi()

    val route: Route = api.route

    val ticketId: UUID = UUID.fromString("52e6d1de-ac56-4cd2-9aa2-2a52ff57cdb5")

    private val tp: TimeProvider = TimeProvider.apply()
    val ticketDetailsDto: TicketDetailsDto = TicketDetailsDto(
      ticketId,
      uid,
      tp.epochMillis,
      TicketType.AdviceRequest,
      "Desc",
      None,
      IndexedSeq.empty[MessageDto],
    )
  }

  "TicketApi" should {
    "POST /tickets/{ticketId}" should {
      "return OK when getting ticket details with Admin authorities" in new TestScope {
        whenF(ticketDetailsService.getTicketDetails(any[UUID], any[String])).thenReturn(ticketDetailsDto)

        Get(s"/tickets/$ticketId") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(ticketDetailsService, times(1)).getTicketDetails(ticketId, uid)
        verify(ticketDetailsService, times(0)).isCreatedByUid(any[UUID], any[String])
        verify(ticketDetailsService, times(0)).isAssignedToUid(any[UUID], any[String])
      }

      "return OK when getting ticket details with Clerk authorities if the topic was created by the Clerk" in new TestScope {
        whenF(ticketDetailsService.isCreatedByUid(any[UUID], any[String])).thenReturn({})
        whenF(ticketDetailsService.getTicketDetails(any[UUID], any[String])).thenReturn(ticketDetailsDto)

        Get(s"/tickets/$ticketId") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(ticketDetailsService, times(1)).getTicketDetails(ticketId, uid)
        verify(ticketDetailsService, times(1)).isCreatedByUid(ticketId, uid)
        verify(ticketDetailsService, times(0)).isAssignedToUid(any[UUID], any[String])
      }

      "return OK when getting ticket details with Clerk authorities if the topic is assigned to the Clerk" in new TestScope {
        private val error = AuthError(s"User with id `$uid` is not an authorized personnel!")
        whenF(ticketDetailsService.isCreatedByUid(any[UUID], any[String])).thenFailWith(error)
        whenF(ticketDetailsService.isAssignedToUid(any[UUID], any[String])).thenReturn({})
        whenF(ticketDetailsService.getTicketDetails(any[UUID], any[String])).thenReturn(ticketDetailsDto)

        Get(s"/tickets/$ticketId") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(ticketDetailsService, times(1)).getTicketDetails(ticketId, uid)
        verify(ticketDetailsService, times(1)).isCreatedByUid(ticketId, uid)
        verify(ticketDetailsService, times(1)).isAssignedToUid(ticketId, uid)
      }

      "return OK when getting ticket details with User authorities if the topic was created by the User" in new TestScope {
        whenF(ticketDetailsService.isCreatedByUid(any[UUID], any[String])).thenReturn({})
        whenF(ticketDetailsService.getTicketDetails(any[UUID], any[String])).thenReturn(ticketDetailsDto)

        Get(s"/tickets/$ticketId") ~> addCredentials(OAuth2BearerToken(validTokenWithUserAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(ticketDetailsService, times(1)).getTicketDetails(ticketId, uid)
        verify(ticketDetailsService, times(1)).isCreatedByUid(ticketId, uid)
        verify(ticketDetailsService, times(0)).isAssignedToUid(any[UUID], any[String])
      }

      "return NotFound if there was no ticket found for the provided ticketId" in new TestScope {
        private val error = TicketNotFound(s"There was no ticket with id `$ticketId` found!")
        whenF(ticketDetailsService.getTicketDetails(any[UUID], any[String])).thenFailWith(error)

        Get(s"/tickets/$ticketId") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.NotFound
          implicit val decoder: Decoder[TicketNotFound] = deriveDecoder
          responseShouldBeDto[TicketNotFound](error)
        }

        verify(ticketDetailsService, times(1)).getTicketDetails(ticketId, uid)
        verify(ticketDetailsService, times(0)).isCreatedByUid(any[UUID], any[String])
        verify(ticketDetailsService, times(0)).isAssignedToUid(any[UUID], any[String])
      }

      "return Unauthorized if ticket was not created by the uid belonging to a User" in new TestScope {
        private val error = AuthError(s"User with id `$uid` is not an authorized personnel!")
        whenF(ticketDetailsService.isCreatedByUid(any[UUID], any[String])).thenFailWith(error)

        Get(s"/tickets/$ticketId") ~> addCredentials(OAuth2BearerToken(validTokenWithUserAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](error)
        }

        verify(ticketDetailsService, times(0)).getTicketDetails(any[UUID], any[String])
        verify(ticketDetailsService, times(1)).isCreatedByUid(ticketId, uid)
        verify(ticketDetailsService, times(0)).isAssignedToUid(any[UUID], any[String])
      }

      "return Unauthorized if ticket was not created by nor assigned to the uid belonging to a Clerk" in new TestScope {
        private val error = AuthError(s"User with id `$uid` is not an authorized personnel!")
        whenF(ticketDetailsService.isCreatedByUid(any[UUID], any[String])).thenFailWith(error)
        whenF(ticketDetailsService.isAssignedToUid(any[UUID], any[String])).thenFailWith(error)

        Get(s"/tickets/$ticketId") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](error)
        }

        verify(ticketDetailsService, times(0)).getTicketDetails(any[UUID], any[String])
        verify(ticketDetailsService, times(1)).isCreatedByUid(ticketId, uid)
        verify(ticketDetailsService, times(1)).isAssignedToUid(ticketId, uid)
      }

      "return Unauthorized if there were invalid credentials provided" in new TestScope {
        Get(s"/tickets/$ticketId") ~> addCredentials(OAuth2BearerToken("in.valid.token")) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("Invalid JWT token!"))
        }

        verify(ticketDetailsService, times(0)).getTicketDetails(any[UUID], any[String])
        verify(ticketDetailsService, times(0)).isCreatedByUid(any[UUID], any[String])
        verify(ticketDetailsService, times(0)).isAssignedToUid(any[UUID], any[String])
      }
    }

    "POST /ticket/{ticketId}/open" should {
      "return OK" in new TestScope {
        whenF(changeTicketStatusService.changeTicketStatus(any[UUID], any[Boolean])).thenReturn({})

        private val uuid = UUID.randomUUID()
        Post(s"/tickets/$uuid/open") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.OK
        }

        verify(changeTicketStatusService, times(1)).changeTicketStatus(uuid, true)
      }

      "return NotFound if there was no ticket found for the provided id" in new TestScope {
        private val uuid  = UUID.randomUUID()
        private val error = TicketNotFound(s"There was no ticket with id `$uuid` found!")
        whenF(changeTicketStatusService.changeTicketStatus(any[UUID], any[Boolean])).thenFailWith(error)

        Post(s"/tickets/$uuid/open") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.NotFound
          implicit val decoder: Decoder[TicketNotFound] = deriveDecoder
          responseShouldBeDto[TicketNotFound](error)
        }

        verify(changeTicketStatusService, times(1)).changeTicketStatus(uuid, true)
      }

      "return InternalServerError if updating the ticket failed" in new TestScope {
        private val uuid  = UUID.randomUUID()
        private val error = DbActionUnsuccessful(s"Updating ticket with id `$uuid` failed!")
        whenF(changeTicketStatusService.changeTicketStatus(any[UUID], any[Boolean])).thenFailWith(error)

        Post(s"/tickets/$uuid/open") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.InternalServerError
          implicit val decoder: Decoder[DbActionUnsuccessful] = deriveDecoder
          responseShouldBeDto[DbActionUnsuccessful](error)
        }

        verify(changeTicketStatusService, times(1)).changeTicketStatus(uuid, true)
      }

      "return Unauthorized if the given credentials did not contain Clerk authorities" in new TestScope {
        Post(s"/tickets/${UUID.randomUUID()}/open") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("No sufficient authorities!"))
        }

        verify(changeTicketStatusService, times(0)).changeTicketStatus(any[UUID], any[Boolean])
      }

      "return Unauthorized if there were no credentials provided" in new TestScope {
        Post(s"/tickets/${UUID.randomUUID()}/open") ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("The user is not authenticated!"))
        }

        verify(changeTicketStatusService, times(0)).changeTicketStatus(any[UUID], any[Boolean])
      }

      "return Unauthorized if there were invalid credentials provided" in new TestScope {
        Post(s"/tickets/${UUID.randomUUID()}/open") ~> addCredentials(OAuth2BearerToken("in.valid.token")) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("Invalid JWT token!"))
        }

        verify(changeTicketStatusService, times(0)).changeTicketStatus(any[UUID], any[Boolean])
      }
    }

    "POST /ticket/{ticketId}/close" should {
      "return OK" in new TestScope {
        whenF(changeTicketStatusService.changeTicketStatus(any[UUID], any[Boolean])).thenReturn({})

        private val uuid = UUID.randomUUID()
        Post(s"/tickets/$uuid/close") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.OK
        }

        verify(changeTicketStatusService, times(1)).changeTicketStatus(uuid, false)
      }

      "return NotFound if there was no ticket found for the provided id" in new TestScope {
        private val uuid  = UUID.randomUUID()
        private val error = TicketNotFound(s"There was no ticket with id `$uuid` found!")
        whenF(changeTicketStatusService.changeTicketStatus(any[UUID], any[Boolean])).thenFailWith(error)

        Post(s"/tickets/$uuid/close") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.NotFound
          implicit val decoder: Decoder[TicketNotFound] = deriveDecoder
          responseShouldBeDto[TicketNotFound](error)
        }

        verify(changeTicketStatusService, times(1)).changeTicketStatus(uuid, false)
      }

      "return InternalServerError if updating the ticket failed" in new TestScope {
        private val uuid  = UUID.randomUUID()
        private val error = DbActionUnsuccessful(s"Updating ticket with id `$uuid` failed!")
        whenF(changeTicketStatusService.changeTicketStatus(any[UUID], any[Boolean])).thenFailWith(error)

        Post(s"/tickets/$uuid/close") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.InternalServerError
          implicit val decoder: Decoder[DbActionUnsuccessful] = deriveDecoder
          responseShouldBeDto[DbActionUnsuccessful](error)
        }

        verify(changeTicketStatusService, times(1)).changeTicketStatus(uuid, false)
      }

      "return Unauthorized if the given credentials did not contain Clerk authorities" in new TestScope {
        Post(s"/tickets/${UUID.randomUUID()}/close") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("No sufficient authorities!"))
        }

        verify(changeTicketStatusService, times(0)).changeTicketStatus(any[UUID], any[Boolean])
      }

      "return Unauthorized if there were no credentials provided" in new TestScope {
        Post(s"/tickets/${UUID.randomUUID()}/close") ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("The user is not authenticated!"))
        }

        verify(changeTicketStatusService, times(0)).changeTicketStatus(any[UUID], any[Boolean])
      }

      "return Unauthorized if there were invalid credentials provided" in new TestScope {
        Post(s"/tickets/${UUID.randomUUID()}/close") ~> addCredentials(OAuth2BearerToken("in.valid.token")) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("Invalid JWT token!"))
        }

        verify(changeTicketStatusService, times(0)).changeTicketStatus(any[UUID], any[Boolean])
      }
    }
  }
}
