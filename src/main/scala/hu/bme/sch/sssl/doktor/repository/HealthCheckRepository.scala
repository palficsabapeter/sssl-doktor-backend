package hu.bme.sch.sssl.doktor.repository

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class HealthCheckRepository(
    implicit
    db: Database,
    ec: ExecutionContext,
) {
  def checkStatus: Future[Boolean] =
    db.run(sql"SELECT 1".as[Int])
      .map(_ => true)
      .recover { case _ => false }
}
