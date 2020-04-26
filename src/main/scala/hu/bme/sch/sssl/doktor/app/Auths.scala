package hu.bme.sch.sssl.doktor.app

import hu.bme.sch.sssl.doktor.auth._
import hu.bme.sch.sssl.doktor.util.TimeProvider

class Auths(config: Config) {
  import config._

  implicit val timeProvider: TimeProvider = TimeProvider.apply()
  implicit val jwtService: JwtService     = new JwtService()
  implicit val jwtAuth: JwtAuth           = new JwtAuth()
}
