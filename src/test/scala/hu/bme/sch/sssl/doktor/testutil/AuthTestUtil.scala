package hu.bme.sch.sssl.doktor.testutil

import hu.bme.sch.sssl.doktor.`enum`.Authorities
import hu.bme.sch.sssl.doktor.app.Config
import hu.bme.sch.sssl.doktor.auth.JwtService.JwtPayload
import hu.bme.sch.sssl.doktor.auth.{JwtAuth, JwtService}
import hu.bme.sch.sssl.doktor.util.TimeProvider

trait AuthTestUtil {
  val config: Config                   = new Config {}
  implicit val jwtConf: Config.JwtConf = config.jwtConf

  implicit val tp: TimeProvider = TimeProvider.apply()

  implicit val jwtService: JwtService = new JwtService()
  implicit val jwtAuth: JwtAuth       = new JwtAuth()

  val uid         = "userId1"
  val user        = "user1"
  val email       = "user1@mail.com"
  val authorities = Seq(Authorities.Admin, Authorities.Clerk, Authorities.User)
  val payload     = JwtPayload(uid, user, email, authorities)
  val validToken  = jwtService.encode(payload)

  val validTokenWithUserAuth  = jwtService.encode(payload.copy(authorities = Seq(Authorities.User)))
  val validTokenWithClerkAuth = jwtService.encode(payload.copy(authorities = Seq(Authorities.Clerk, Authorities.User)))
}
