package hu.bme.sch.sssl.doktor

import akka.actor.ActorSystem
import hu.bme.sch.sssl.doktor.app._
import hu.bme.sch.sssl.doktor.util.LoggerUtil
import org.slf4j.Logger

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object Main extends App {
  LoggerUtil.initBridge()

  private val logger: Logger = LoggerUtil.getLogger(getClass)

  implicit private val system: ActorSystem                        = ActorSystem("sssl-doktor")
  implicit private val executionContext: ExecutionContextExecutor = system.dispatcher

  private val config   = new Config()
  private val services = new Services()
  private val apis     = new Apis(services)

  private lazy val starting = for {
    _      <- Migrator.run(config.migratorConf)
    server <- apis.bindApis()
  } yield server

  starting.onComplete {
    case Success(_) =>
      logger.info("Application startup successful!")
    case Failure(exception) =>
      logger.error("Application startup failed!", exception)
      system.terminate()
  }
}
