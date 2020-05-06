package hu.bme.sch.sssl.doktor.app

import akka.actor.ActorSystem
import hu.bme.sch.sssl.doktor.service._
import hu.bme.sch.sssl.doktor.util.{TimeProvider, UuidProvider}

import scala.concurrent.ExecutionContext

class Services(config: Config, repositories: Repositories, auths: Auths)(
    implicit
    ec: ExecutionContext,
    as: ActorSystem,
) {
  import auths._
  import config._
  import repositories._

  implicit val uuidProvider: UuidProvider = UuidProvider.apply()

  implicit val healthCheckService: HealthCheckService = new HealthCheckService()
  implicit val loginService: LoginService             = new LoginService()
  implicit val newTicketService: NewTicketService     = new NewTicketService()
  implicit val myTicketsService: MyTicketsService     = new MyTicketsService()
  implicit val allTicketsService: AllTicketsService   = new AllTicketsService()
}
