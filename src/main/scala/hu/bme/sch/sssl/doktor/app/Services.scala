package hu.bme.sch.sssl.doktor.app

import hu.bme.sch.sssl.doktor.service.HealthCheckService

import scala.concurrent.ExecutionContext

class Services(
    implicit
    ec: ExecutionContext,
) {
  implicit val healthCheckService: HealthCheckService = new HealthCheckService()
}
