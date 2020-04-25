package hu.bme.sch.sssl.doktor.app

import hu.bme.sch.sssl.doktor.service.HealthCheckService

import scala.concurrent.ExecutionContext

class Services(repositories: Repositories)(
    implicit
    ec: ExecutionContext,
) {
  import repositories._

  implicit val healthCheckService: HealthCheckService = new HealthCheckService()
}
