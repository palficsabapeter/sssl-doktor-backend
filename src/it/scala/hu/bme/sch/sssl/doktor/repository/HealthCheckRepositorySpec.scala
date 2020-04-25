package hu.bme.sch.sssl.doktor.repository

import hu.bme.sch.sssl.doktor.testutil.RepositoryTestBase

import scala.concurrent.Future

class HealthCheckRepositorySpec extends RepositoryTestBase {
  override def cleanDb(): Future[_] = Future.successful()

  trait TestScope {
    val repo: HealthCheckRepository = new HealthCheckRepository()
  }

  "HealthCheckRepository" should {
    "#checkStatus" should {
      "return true" in new TestScope {
        await(repo.checkStatus) shouldBe true
      }
    }
  }
}
