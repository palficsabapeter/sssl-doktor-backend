package hu.bme.sch.sssl.doktor.testutil

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.BeforeAndAfterEach

trait WireMockTestBase extends TestBase with BeforeAndAfterEach {
  def hostName: String
  def port: Int

  lazy val mockServer: WireMockServer = new WireMockServer(wireMockConfig().bindAddress(hostName).port(port))

  override def beforeEach(): Unit =
    mockServer.start()

  override def afterEach(): Unit =
    mockServer.stop()
}
