package hu.bme.sch.sssl.doktor.service

import cats.data.EitherT
import cats.implicits._
import hu.bme.sch.sssl.doktor.BuildInfo
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AppError, AppErrorOr}

import scala.concurrent.{ExecutionContext, Future}

class HealthCheckService(
    implicit
    ec: ExecutionContext,
) {
  import HealthCheckService._

  def checkStatus: AppErrorOr[HealthCheckDto] =
    EitherT.rightT[Future, AppError](
      HealthCheckDto(
        true,
        BuildInfo.version,
        BuildInfo.builtAtString,
        BuildInfo.builtAtMillis,
        BuildInfo.commitHash,
      ),
    )
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
