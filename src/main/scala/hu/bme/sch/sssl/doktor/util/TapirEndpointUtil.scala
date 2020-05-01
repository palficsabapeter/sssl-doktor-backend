package hu.bme.sch.sssl.doktor.util

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AppError, AuthError}
import io.circe.Decoder
import sttp.model.StatusCode
import sttp.tapir.EndpointIO.Header
import sttp.tapir.server.{DecodeFailureHandling, ServerDefaults, ServerEndpoint}

import scala.concurrent.Future

object TapirEndpointUtil {
  implicit class TapirEndpointTransformer(tapirEndpoints: List[ServerEndpoint[_, _, _, Nothing, Future]]) {
    def convertTapirEndpointsToAkkaRoutes: Route = {
      import sttp.tapir.server.akkahttp._

      implicit val customServerOptions: AkkaHttpServerOptions = AkkaHttpServerOptions.default.copy(
        decodeFailureHandler = ctx => {
          import io.circe.generic.auto._
          import sttp.tapir._
          import sttp.tapir.json.circe._

          ctx.input match {
            case h: Header[_] if h.name == "Authorization" =>
              DecodeFailureHandling.response(
                statusCode.and(jsonBody[AuthError]),
              )(
                (StatusCode.Unauthorized, AuthError("The user is not authenticated!")),
              )
            case _ =>
              ServerDefaults.decodeFailureHandler(ctx)
          }
        },
      )

      tapirEndpoints.toRoute
    }
  }

  final def unmarshaller[A: Decoder]: FromEntityUnmarshaller[A] = {
    import io.circe.parser.decode

    Unmarshaller.stringUnmarshaller
      .flatMap(_ => _ => json => decode[A](json).fold(Future.failed, Future.successful))
  }
}
