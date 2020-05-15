package hu.bme.sch.sssl.doktor.service

import hu.bme.sch.sssl.doktor.repository.HealthCheckRepository
import hu.bme.sch.sssl.doktor.service.HealthCheckService.HealthCheckDto
import hu.bme.sch.sssl.doktor.testutil.TestBase
import hu.bme.sch.sssl.doktor.util.ErrorUtil.DbUnavailable

import scala.concurrent.Future

class HealthCheckServiceSpec extends TestBase {
  trait TestScope {
    implicit val repo: HealthCheckRepository = mock[HealthCheckRepository]

    val service: HealthCheckService = new HealthCheckService()
  }

  "HealthCheckService" should {
    "#checkStatus" should {
      "return a HealthCheckDto if DB check was successful" in new TestScope {
        when(repo.checkStatus).thenReturn(Future.successful(true))

        await(service.checkStatus.value) shouldBe a[Right[_, HealthCheckDto]]
      }

      "return a DbUnavailable error if DB was unavailable" in new TestScope {
        private val error = DbUnavailable("The database did not produce a timely response!")
        when(repo.checkStatus).thenReturn(Future.successful(false))

        await(service.checkStatus.value) shouldBe Left(error)
      }
    }
  }
}
