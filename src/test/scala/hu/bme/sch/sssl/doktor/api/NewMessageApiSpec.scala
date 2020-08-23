package hu.bme.sch.sssl.doktor.api

import java.util.UUID

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.MessageStatus
import hu.bme.sch.sssl.doktor.api.NewMessageApi.MessageTextDto
import hu.bme.sch.sssl.doktor.service.NewMessageService
import hu.bme.sch.sssl.doktor.service.NewMessageService.CreateMessageDto
import hu.bme.sch.sssl.doktor.testutil.{ApiTestBase, AuthTestUtil}
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AuthError, DbActionUnsuccessful, TicketNotFound}

class NewMessageApiSpec extends ApiTestBase with AuthTestUtil {
  import io.circe.Decoder
  import io.circe.generic.auto._
  import io.circe.generic.semiauto._
  import io.circe.syntax._

  trait TestScope {
    implicit val service: NewMessageService = mock[NewMessageService]

    val api: NewMessageApi = new NewMessageApi()
    val route: Route       = api.route

    val ticketId: UUID = UUID.fromString("52e6d1de-ac56-4cd2-9aa2-2a52ff57cdb5")

    val dto: MessageTextDto = MessageTextDto("Text")

    val reqEntity: HttpEntity.Strict = HttpEntity(ContentTypes.`application/json`, dto.asJson.toString)

    val createDto: CreateMessageDto = CreateMessageDto(
      ticketId,
      uid,
      user,
      MessageStatus.Shown,
      "Text",
    )
  }

  "NewMessageApi" should {
    "POST /tickets/{ticketId}/messages" should {
      "return OK when creating a message with Admin authorities" in new TestScope {
        whenF(service.createMessage(any[CreateMessageDto])).thenReturn({})

        Post(s"/tickets/$ticketId/messages").withEntity(reqEntity) ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(service, times(1)).createMessage(createDto)
        verify(service, times(0)).isCreatedByUid(any[UUID], any[String])
        verify(service, times(0)).isAssignedToUid(any[UUID], any[String])
      }

      "return OK when creating a message with Clerk authorities if the topic was created by the Clerk" in new TestScope {
        whenF(service.isCreatedByUid(any[UUID], any[String])).thenReturn({})
        whenF(service.createMessage(any[CreateMessageDto])).thenReturn({})

        Post(s"/tickets/$ticketId/messages").withEntity(reqEntity) ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(service, times(1)).createMessage(createDto)
        verify(service, times(1)).isCreatedByUid(ticketId, uid)
        verify(service, times(0)).isAssignedToUid(any[UUID], any[String])
      }

      "return OK when creating a message with Clerk authorities if the topic is assigned to the Clerk" in new TestScope {
        private val error = AuthError(s"User with id `$uid` is not an authorized personnel!")
        whenF(service.isCreatedByUid(any[UUID], any[String])).thenFailWith(error)
        whenF(service.isAssignedToUid(any[UUID], any[String])).thenReturn({})
        whenF(service.createMessage(any[CreateMessageDto])).thenReturn({})

        Post(s"/tickets/$ticketId/messages").withEntity(reqEntity) ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(service, times(1)).createMessage(createDto.copy(status = MessageStatus.Unreviewed))
        verify(service, times(1)).isCreatedByUid(ticketId, uid)
        verify(service, times(1)).isAssignedToUid(ticketId, uid)
      }

      "return OK when creating a message with User authorities if the topic was created by the User" in new TestScope {
        whenF(service.isCreatedByUid(any[UUID], any[String])).thenReturn({})
        whenF(service.createMessage(any[CreateMessageDto])).thenReturn({})

        Post(s"/tickets/$ticketId/messages").withEntity(reqEntity) ~> addCredentials(OAuth2BearerToken(validTokenWithUserAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(service, times(1)).createMessage(createDto)
        verify(service, times(1)).isCreatedByUid(ticketId, uid)
        verify(service, times(0)).isAssignedToUid(any[UUID], any[String])
      }

      "return NotFound if there was no ticket found for the provided ticketId" in new TestScope {
        private val error = TicketNotFound(s"There was no ticket with id `$ticketId` found!")
        whenF(service.createMessage(any[CreateMessageDto])).thenFailWith(error)

        Post(s"/tickets/$ticketId/messages").withEntity(reqEntity) ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.NotFound
          implicit val decoder: Decoder[TicketNotFound] = deriveDecoder
          responseShouldBeDto[TicketNotFound](error)
        }

        verify(service, times(1)).createMessage(createDto)
        verify(service, times(0)).isCreatedByUid(any[UUID], any[String])
        verify(service, times(0)).isAssignedToUid(any[UUID], any[String])
      }

      "return InternalServerError if inserting message failed" in new TestScope {
        private val error = DbActionUnsuccessful(s"Inserting message for ticket with id `$ticketId` failed!")
        whenF(service.createMessage(any[CreateMessageDto])).thenFailWith(error)

        Post(s"/tickets/$ticketId/messages").withEntity(reqEntity) ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.InternalServerError
          implicit val decoder: Decoder[DbActionUnsuccessful] = deriveDecoder
          responseShouldBeDto[DbActionUnsuccessful](error)
        }

        verify(service, times(1)).createMessage(createDto)
        verify(service, times(0)).isCreatedByUid(any[UUID], any[String])
        verify(service, times(0)).isAssignedToUid(any[UUID], any[String])
      }

      "return Unauthorized if ticket was not created by the uid belonging to a User" in new TestScope {
        private val error = AuthError(s"User with id `$uid` is not an authorized personnel!")
        whenF(service.isCreatedByUid(any[UUID], any[String])).thenFailWith(error)

        Post(s"/tickets/$ticketId/messages").withEntity(reqEntity) ~> addCredentials(OAuth2BearerToken(validTokenWithUserAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](error)
        }

        verify(service, times(0)).createMessage(any[CreateMessageDto])
        verify(service, times(1)).isCreatedByUid(ticketId, uid)
        verify(service, times(0)).isAssignedToUid(any[UUID], any[String])
      }

      "return Unauthorized if ticket was not created by nor assigned to the uid belonging to a Clerk" in new TestScope {
        private val error = AuthError(s"User with id `$uid` is not an authorized personnel!")
        whenF(service.isCreatedByUid(any[UUID], any[String])).thenFailWith(error)
        whenF(service.isAssignedToUid(any[UUID], any[String])).thenFailWith(error)

        Post(s"/tickets/$ticketId/messages").withEntity(reqEntity) ~> addCredentials(OAuth2BearerToken(validTokenWithClerkAuth)) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](error)
        }

        verify(service, times(0)).createMessage(any[CreateMessageDto])
        verify(service, times(1)).isCreatedByUid(ticketId, uid)
        verify(service, times(1)).isAssignedToUid(ticketId, uid)
      }

      "return Unauthorized if there were invalid credentials provided" in new TestScope {
        Post(s"/tickets/$ticketId/messages").withEntity(reqEntity) ~> addCredentials(OAuth2BearerToken("in.valid.token")) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("Invalid JWT token!"))
        }

        verify(service, times(0)).createMessage(any[CreateMessageDto])
        verify(service, times(0)).isCreatedByUid(any[UUID], any[String])
        verify(service, times(0)).isAssignedToUid(any[UUID], any[String])
      }
    }
  }
}
