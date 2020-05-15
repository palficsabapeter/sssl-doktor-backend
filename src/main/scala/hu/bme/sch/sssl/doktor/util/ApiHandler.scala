package hu.bme.sch.sssl.doktor.util

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, extractUri}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import hu.bme.sch.sssl.doktor.util.ErrorUtil._
import io.circe.generic.auto._
import io.circe.syntax._
import org.slf4j.Logger

object ApiHandler {
  implicit def exceptionHandler(
      implicit
      logger: Logger,
  ): ExceptionHandler =
    ExceptionHandler {
      case exception =>
        extractUri { uri =>
          logger.warn(s"Request to $uri could not be handled normally!", exception)
          cors() {
            complete(HttpResponse(StatusCodes.InternalServerError))
          }
        }
    }

  /**
    * mostly based on
    * https://doc.akka.io/docs/akka-http/current/routing-dsl/rejections.html#customising-rejection-http-responses
    */
  implicit def rejectionHandler: RejectionHandler =
    corsRejectionHandler
      .mapRejectionResponse {
        case res @ HttpResponse(_, _, ent: HttpEntity.Strict, _) =>
          // since all Akka default rejection responses are Strict this will handle all rejections
          val error: AppError = UnsuccessfulAction(ent.data.utf8String)
          val body: String    = error.asJson.toString

          // we copy the response in order to keep all headers and status code, wrapping the message as hand rolled JSON
          // you could the entity using your favourite marshalling library (e.g. spray json, circe or anything else)
          res.copy(entity = HttpEntity(ContentTypes.`application/json`, body))
        case x => x // pass through all other types of responses
      }
}
