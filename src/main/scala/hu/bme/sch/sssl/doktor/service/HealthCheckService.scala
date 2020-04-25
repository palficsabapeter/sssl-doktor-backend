package hu.bme.sch.sssl.doktor.service

import cats.data.EitherT
import hu.bme.sch.sssl.doktor.BuildInfo
import hu.bme.sch.sssl.doktor.repository.HealthCheckRepository
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AppErrorOr, DbUnavailable}

import scala.concurrent.ExecutionContext

class HealthCheckService(
    implicit
    repo: HealthCheckRepository,
    ec: ExecutionContext,
) {
  import HealthCheckService._

  def checkStatus: AppErrorOr[HealthCheckDto] = {
    val res = for {
      status <- repo.checkStatus
    } yield {
      val dto = HealthCheckDto(
        status,
        BuildInfo.version,
        BuildInfo.builtAtString,
        BuildInfo.builtAtMillis,
        BuildInfo.commitHash,
      )

      Either.cond(status, dto, DbUnavailable("The database did not produce a timely response!"))
    }

    EitherT(res)
  }
}

object HealthCheckService {
  case class HealthCheckDto(
      status: Boolean,
      version: String,
      builtAtString: String,
      builtAtMillis: Long,
      commitHash: Option[String],
  )
}
