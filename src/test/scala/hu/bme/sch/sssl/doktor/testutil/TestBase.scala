package hu.bme.sch.sssl.doktor.testutil

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.mockito.cats.MockitoCats
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait TestBase extends AnyWordSpecLike with Matchers with MockitoSugar with MockitoCats with ScalatestRouteTest with ArgumentMatchersSugar {
  def await[T](f: Future[T]): T =
    Await.result(f, 5.seconds)
}
