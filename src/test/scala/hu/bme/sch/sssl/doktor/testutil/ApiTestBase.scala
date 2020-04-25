package hu.bme.sch.sssl.doktor.testutil

import akka.http.scaladsl.model.{ContentTypeRange, MediaTypes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal, Unmarshaller}
import hu.bme.sch.sssl.doktor.api.ApiBase
import hu.bme.sch.sssl.doktor.util.TapirEndpointUtil
import io.circe.Decoder
import org.scalatest.Assertion

import scala.concurrent.Future

trait ApiTestBase extends TestBase {
  implicit class TapirApiToAkkaRoute(api: ApiBase) {
    import TapirEndpointUtil._

    def route: Route = TapirEndpointTransformer(api.endpoints).convertTapirEndpointsToAkkaRoutes
  }

  final def unmarshaller[A: Decoder]: FromEntityUnmarshaller[A] = {
    import io.circe.parser.decode

    val jsonContentTypes: List[ContentTypeRange] = List(MediaTypes.`application/json`)

    Unmarshaller.stringUnmarshaller
      .forContentTypes(jsonContentTypes: _*)
      .flatMap(_ => _ => json => decode[A](json).fold(Future.failed, Future.successful))
  }

  final def responseShouldBeDto[A](dto: A)(
      implicit
      decoder: Decoder[A],
  ): Assertion = {
    implicit val dtoUnmarshaller: FromEntityUnmarshaller[A] = unmarshaller[A]
    await(Unmarshal(response).to[A]) shouldBe dto
  }
}
