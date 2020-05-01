package hu.bme.sch.sssl.doktor.api

import java.util.UUID

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.TicketType
import hu.bme.sch.sssl.doktor.api.TicketsApi.CreateTicketDto
import hu.bme.sch.sssl.doktor.service.MyTicketsService.{MyTicketsResponseDto, TicketOverviewDto}
import hu.bme.sch.sssl.doktor.service.{MyTicketsService, NewTicketService}
import hu.bme.sch.sssl.doktor.testutil.{ApiTestBase, AuthTestUtil}
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AuthError, DbActionUnsuccessful}

class TicketsApiSpec extends ApiTestBase with AuthTestUtil {
  import io.circe.Decoder
  import io.circe.generic.auto._
  import io.circe.generic.semiauto._
  import io.circe.syntax._

  trait TestScope {
    implicit val newTicketService: NewTicketService = mock[NewTicketService]
    implicit val myTicketsService: MyTicketsService = mock[MyTicketsService]

    val api: TicketsApi = new TicketsApi()

    val route: Route = api.route

    val dto: CreateTicketDto = CreateTicketDto(
      TicketType.Criticism,
      false,
      "Description",
    )

    val reqEntity: HttpEntity.Strict = HttpEntity(ContentTypes.`application/json`, dto.asJson.toString)
  }

  "TicketsApi" should {
    "POST /tickets" should {
      "return OK" in new TestScope {
        whenF(newTicketService.createTicket(any[String], any[String], any[CreateTicketDto])).thenReturn({})

        Post("/tickets").withEntity(reqEntity) ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }

        verify(newTicketService, times(1)).createTicket(uid, user, dto)
      }

      "return InternalServerError with DbActionUnsuccessful error" in new TestScope {
        private val error = DbActionUnsuccessful("Could not insert ticket into database!")
        whenF(newTicketService.createTicket(any[String], any[String], any[CreateTicketDto])).thenFailWith(error)

        Post("/tickets").withEntity(reqEntity) ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.InternalServerError
          implicit val decoder: Decoder[DbActionUnsuccessful] = deriveDecoder
          responseShouldBeDto[DbActionUnsuccessful](error)
        }

        verify(newTicketService, times(1)).createTicket(uid, user, dto)
      }

      "return Unauthorized if there were no credentials provided" in new TestScope {
        Post("/tickets").withEntity(reqEntity) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("The user is not authenticated!"))
        }

        verify(newTicketService, times(0)).createTicket(any[String], any[String], any[CreateTicketDto])
      }

      "return Unauthorized if there were invalid credentials provided" in new TestScope {
        Post("/tickets").withEntity(reqEntity) ~> addCredentials(OAuth2BearerToken("in.valid.token")) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("Invalid JWT token!"))
        }

        verify(newTicketService, times(0)).createTicket(any[String], any[String], any[CreateTicketDto])
      }

      "return BadRequest if request body was not provided" in new TestScope {
        Post("/tickets") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldBe StatusCodes.BadRequest
        }

        verify(newTicketService, times(0)).createTicket(any[String], any[String], any[CreateTicketDto])
      }
    }

    "GET /tickets" should {
      "return OK with MyTicketsResponseDto" in new TestScope {
        private val response = MyTicketsResponseDto(
          for {
            i <- 2 to 11
          } yield TicketOverviewDto(
            UUID.randomUUID(),
            0L,
            Some(user),
            TicketType.AdviceRequest,
            Some(s"userId$i"),
          ),
          for {
            i <- 1 to 10
          } yield TicketOverviewDto(
            UUID.randomUUID(),
            0L,
            None,
            TicketType.FeedbackRequest,
            Some(user),
          ),
        )
        whenF(myTicketsService.getMyTickets(any[String])).thenReturn(response)

        Get("/tickets") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
          implicit val decoder: Decoder[MyTicketsResponseDto] = deriveDecoder
          responseShouldBeDto[MyTicketsResponseDto](response)
        }

        verify(myTicketsService, times(1)).getMyTickets(uid)
      }

      "return Unauthorized if there were no credentials provided" in new TestScope {
        Get("/tickets") ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("The user is not authenticated!"))
        }

        verify(myTicketsService, times(0)).getMyTickets(any[String])
      }

      "return Unauthorized if there were invalid credentials provided" in new TestScope {
        Get("/tickets") ~> addCredentials(OAuth2BearerToken("in.valid.token")) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
          implicit val decoder: Decoder[AuthError] = deriveDecoder
          responseShouldBeDto[AuthError](AuthError("Invalid JWT token!"))
        }

        verify(myTicketsService, times(0)).getMyTickets(any[String])
      }
    }
  }
}
