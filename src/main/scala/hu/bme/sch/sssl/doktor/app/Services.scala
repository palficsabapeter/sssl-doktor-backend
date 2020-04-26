package hu.bme.sch.sssl.doktor.app

import akka.actor.ActorSystem
import hu.bme.sch.sssl.doktor.service._

import scala.concurrent.ExecutionContext

class Services(config: Config, repositories: Repositories, auths: Auths)(
    implicit
    ec: ExecutionContext,
    as: ActorSystem,
) {
  import auths._
  import config._
  import repositories._

  implicit val healthCheckService: HealthCheckService = new HealthCheckService()
  implicit val loginService: LoginService             = new LoginService()
}
