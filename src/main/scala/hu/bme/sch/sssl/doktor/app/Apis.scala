package hu.bme.sch.sssl.doktor.app

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import hu.bme.sch.sssl.doktor.api.ApiBase
import hu.bme.sch.sssl.doktor.util.{ApiHandler, LoggerUtil}
import org.slf4j.Logger

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class Apis(
    implicit
    ec: ExecutionContext,
    as: ActorSystem,
) {
  implicit val logger: Logger = LoggerUtil.getLogger(this.getClass)

  def route: Route =
    pathPrefix("api") {
      val emptyRoute = Route(_.reject())

      val apis = Seq.empty[ApiBase]

      cors() {
        apis
          .map(_.route)
          .fold(emptyRoute)(_ ~ _)
      }
    }

  def bindApis(): Future[Http.ServerBinding] = {
    import ApiHandler.{exceptionHandler, rejectionHandler}

    val bind: Future[Http.ServerBinding] = Http()
      .bindAndHandle(route, "0.0.0.0", 9000)

    bind.onComplete {
      case Success(server) =>
        setupShutdownHook(server)
        logger.info("Listening on port 9000!")
      case Failure(ex) => logger.error("Api binding failed!", ex)
    }

    bind
  }

  private def setupShutdownHook(server: Http.ServerBinding): Unit =
    CoordinatedShutdown(as).addTask(CoordinatedShutdown.PhaseServiceUnbind, "http_shutdown") { () =>
      server.terminate(hardDeadline = 10.seconds).map(_ => Done)
    }
}
