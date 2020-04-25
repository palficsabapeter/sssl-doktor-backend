package hu.bme.sch.sssl.doktor.app

import hu.bme.sch.sssl.doktor.repository.HealthCheckRepository
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

class Repositories(config: Config)(
    implicit
    ec: ExecutionContext,
) {
  implicit val db: Database                                 = config.dbConf.db
  implicit val healthCheckRepository: HealthCheckRepository = new HealthCheckRepository()
}
