package hu.bme.sch.sssl.doktor.api

import hu.bme.sch.sssl.doktor.service.HealthCheckService
import hu.bme.sch.sssl.doktor.service.HealthCheckService.HealthCheckDto
import hu.bme.sch.sssl.doktor.util.LoggerUtil
import org.slf4j.Logger

class HealthCheckApi(
    implicit
    service: HealthCheckService,
) extends ApiBase {
  import io.circe.generic.auto._
  import sttp.tapir._
  import sttp.tapir.json.circe._

  implicit val logger: Logger = LoggerUtil.getLogger(getClass)

  def endpoints =
    List(
      endpoint.get
        .in("healthCheck")
        .out(jsonBody[HealthCheckDto])
        .withGeneralErrorHandler()
        .serverLogic(_ => service.checkStatus.value),
    )
}
