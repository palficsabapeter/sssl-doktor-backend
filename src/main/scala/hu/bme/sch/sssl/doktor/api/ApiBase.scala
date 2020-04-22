package hu.bme.sch.sssl.doktor.api

import akka.http.scaladsl.server.Route

trait ApiBase {
  def route: Route
}
