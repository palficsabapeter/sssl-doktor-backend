package hu.bme.sch.sssl.doktor.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import cats.implicits._
import hu.bme.sch.sssl.doktor.service.HealthCheckService
import hu.bme.sch.sssl.doktor.service.HealthCheckService.HealthCheckDto
import hu.bme.sch.sssl.doktor.testutil.ApiTestBase
import hu.bme.sch.sssl.doktor.util.ErrorUtil.DbUnavailable

class HealthCheckApiSpec extends ApiTestBase {
  import io.circe.Decoder
  import io.circe.generic.semiauto._

  trait TestScope {
    implicit val service: HealthCheckService = mock[HealthCheckService]
    val api: HealthCheckApi                  = new HealthCheckApi()

    val route: Route = api.route
  }

  "HealthCheckApi" should {
    "GET /healthCheck" should {
      "return OK with HealthCheckDto" in new TestScope {
        private val dto = HealthCheckDto(
          true,
          "0.1",
          "builtAtString",
          0L,
          Some("commitHash"),
        )

        whenF(service.checkStatus).thenReturn(dto)

        Get("/healthCheck") ~> route ~> check {
          status shouldEqual StatusCodes.OK
          implicit val decoder: Decoder[HealthCheckDto] = deriveDecoder
          responseShouldBeDto[HealthCheckDto](dto)
        }
      }

      "return ServiceUnavailable with DbUnavailable error" in new TestScope {
        private val error = DbUnavailable("The database did not produce a timely response!")
        whenF(service.checkStatus).thenFailWith(error)

        Get("/healthCheck") ~> route ~> check {
          status shouldEqual StatusCodes.ServiceUnavailable
          implicit val decoder: Decoder[DbUnavailable] = deriveDecoder
          responseShouldBeDto[DbUnavailable](error)
        }
      }
    }
  }
}
