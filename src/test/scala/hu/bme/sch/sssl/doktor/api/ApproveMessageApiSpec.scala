package hu.bme.sch.sssl.doktor.api

import java.util.UUID

import cats.implicits._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Route
import hu.bme.sch.sssl.doktor.service.ApproveMessageService
import hu.bme.sch.sssl.doktor.testutil.{ApiTestBase, AuthTestUtil}
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AuthError, MessageNotFound}
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

class ApproveMessageApiSpec extends ApiTestBase with AuthTestUtil {
  trait TestScope {
    implicit val service: ApproveMessageService = mock[ApproveMessageService]

    val api: ApproveMessageApi = new ApproveMessageApi()
    val route: Route           = api.route

    val ticketId: UUID  = UUID.randomUUID()
    val messageId: UUID = UUID.randomUUID()
  }

  "ApproveMessageApi" should {
    "POST /tickets/{ticketId}/messages/{messageId}/approve" should {
      "return OK" in new TestScope {
        whenF(service.approveMessage(any[UUID], any[UUID], any[Boolean], any[String], any[String])).thenReturn({})

        Post(s"/tickets/$ticketId/messages/$messageId/approve") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(service).approveMessage(ticketId, messageId, true, user, uid)
      }

      "return NotFound if there was no message found for the provided ids" in new TestScope {
        private val error = MessageNotFound(s"Message with id '$messageId' and related ticket id '$ticketId' was not found!")
        whenF(service.approveMessage(any[UUID], any[UUID], any[Boolean], any[String], any[String])).thenFailWith(error)

        Post(s"/tickets/$ticketId/messages/$messageId/approve") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.NotFound
          implicit val decoder: Decoder[MessageNotFound] = deriveDecoder
          responseShouldBeDto[MessageNotFound](error)
        }

        verify(service).approveMessage(ticketId, messageId, true, user, uid)
      }

      "return Unauthorized if the user did not have Admin authority" in new TestScope {
        Post(s"/tickets/$ticketId/messages/$messageId/approve") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("No sufficient authorities!"))
        }

        verify(service, times(0)).approveMessage(any[UUID], any[UUID], any[Boolean], any[String], any[String])
      }

      "return Unauthorized if there were invalid credentials provided" in new TestScope {
        Post(s"/tickets/$ticketId/messages/$messageId/approve") ~> addCredentials(OAuth2BearerToken("invalidToken")) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("Invalid JWT token!"))
        }

        verify(service, times(0)).approveMessage(any[UUID], any[UUID], any[Boolean], any[String], any[String])
      }
    }

    "POST /tickets/{ticketId}/messages/{messageId}/decline" should {
      "return OK" in new TestScope {
        whenF(service.approveMessage(any[UUID], any[UUID], any[Boolean], any[String], any[String])).thenReturn({})

        Post(s"/tickets/$ticketId/messages/$messageId/decline") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(service).approveMessage(ticketId, messageId, false, user, uid)
      }

      "return NotFound if there was no message found for the provided ids" in new TestScope {
        private val error = MessageNotFound(s"Message with id '$messageId' and related ticket id '$ticketId' was not found!")
        whenF(service.approveMessage(any[UUID], any[UUID], any[Boolean], any[String], any[String])).thenFailWith(error)

        Post(s"/tickets/$ticketId/messages/$messageId/decline") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.NotFound
          implicit val decoder: Decoder[MessageNotFound] = deriveDecoder
          responseShouldBeDto[MessageNotFound](error)
        }

        verify(service).approveMessage(ticketId, messageId, false, user, uid)
      }

      "return Unauthorized if the user did not have Admin authority" in new TestScope {
        Post(s"/tickets/$ticketId/messages/$messageId/decline") ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("No sufficient authorities!"))
        }

        verify(service, times(0)).approveMessage(any[UUID], any[UUID], any[Boolean], any[String], any[String])
      }

      "return Unauthorized if there were invalid credentials provided" in new TestScope {
        Post(s"/tickets/$ticketId/messages/$messageId/decline") ~> addCredentials(OAuth2BearerToken("invalidToken")) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("Invalid JWT token!"))
        }

        verify(service, times(0)).approveMessage(any[UUID], any[UUID], any[Boolean], any[String], any[String])
      }
    }
  }
}
