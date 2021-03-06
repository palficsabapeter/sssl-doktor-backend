package hu.bme.sch.sssl.doktor.app

import hu.bme.sch.sssl.doktor.repository._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

class Repositories(config: Config)(
    implicit
    ec: ExecutionContext,
) {
  implicit val db: Database                                 = config.dbConf.db
  implicit val healthCheckRepository: HealthCheckRepository = new HealthCheckRepository()
  implicit val authRepository: AuthRepository               = new AuthRepository()
  implicit val ticketRepository: TicketRepository           = new TicketRepository()
  implicit val messageRepository: MessageRepository         = new MessageRepository()
}
