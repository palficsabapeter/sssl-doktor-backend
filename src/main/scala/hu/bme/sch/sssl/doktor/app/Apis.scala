package hu.bme.sch.sssl.doktor.app

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import hu.bme.sch.sssl.doktor.api.HealthCheckApi
import hu.bme.sch.sssl.doktor.util.{ApiHandler, LoggerUtil, TapirEndpointUtil}
import org.slf4j.Logger
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class Apis(services: Services)(
    implicit
    ec: ExecutionContext,
    as: ActorSystem,
) {
  import TapirEndpointUtil._
  import services._

  implicit val logger: Logger = LoggerUtil.getLogger(this.getClass)

  def route: Route =
    pathPrefix("api") {
      val endpoints: List[ServerEndpoint[_, _, _, Nothing, Future]] = List(
        new HealthCheckApi().endpoints,
      ).flatten

      val swaggerRoute: Route = {
        import sttp.tapir.docs.openapi._
        import sttp.tapir.openapi.circe.yaml._
        import sttp.tapir.swagger.akkahttp.SwaggerAkka

        val docsAsYaml: String = endpoints.toOpenAPI("SSSL Doktor Backend", "0.1").toYaml
        new SwaggerAkka(docsAsYaml).routes
      }

      cors() {
        TapirEndpointTransformer(endpoints).convertTapirEndpointsToAkkaRoutes ~ swaggerRoute
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
    CoordinatedShutdown(as).addTask(CoordinatedShutdown.PhaseServiceUnbind, "http_shutdown")(() => server.terminate(hardDeadline = 10.seconds).map(_ => Done))
}
