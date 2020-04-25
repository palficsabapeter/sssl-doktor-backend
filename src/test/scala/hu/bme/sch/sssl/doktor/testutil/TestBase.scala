package hu.bme.sch.sssl.doktor.testutil

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.mockito.MockitoSugar
import org.mockito.cats.MockitoCats
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait TestBase extends AnyWordSpecLike with Matchers with MockitoSugar with MockitoCats with ScalatestRouteTest {
  def await[T](f: Future[T]): T =
    Await.result(f, 5.seconds)
}
