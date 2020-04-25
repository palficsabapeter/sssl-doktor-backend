package hu.bme.sch.sssl.doktor.app

import hu.bme.sch.sssl.doktor.app.Config.MigratorConf
import hu.bme.sch.sssl.doktor.util.LoggerUtil
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Migrator {
  private val logger: Logger = LoggerUtil.getLogger(getClass)

  def run(migratorConf: MigratorConf)(
      implicit
      ec: ExecutionContext,
  ): Future[Int] = {
    val result = Future {
      val flywayConfig: FluentConfiguration = Flyway
        .configure()
        .table("migration_log")
        .dataSource(migratorConf.url, migratorConf.user, migratorConf.password)

      val flyway: Flyway = new Flyway(flywayConfig)

      flyway.migrate()
    }

    result.onComplete {
      case Success(_)         => logger.info("Migration successful!")
      case Failure(exception) => logger.error("Migration failed!", exception)
    }

    result
  }
}
