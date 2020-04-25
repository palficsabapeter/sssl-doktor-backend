package hu.bme.sch.sssl.doktor.auth

import hu.bme.sch.sssl.doktor.`enum`.Authorities.Authorities
import hu.bme.sch.sssl.doktor.app.Config.JwtConf
import hu.bme.sch.sssl.doktor.util.TimeProvider
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtSprayJson}
import spray.json._

class JwtService(
    implicit
    jwtConf: JwtConf,
    tp: TimeProvider,
) {
  import JwtService._

  def encode(uid: String, user: String, email: String, fullname: String, authorities: Seq[Authorities]): String = {
    val now = tp.epochSecs
    val claim = JwtClaim(JwtPayload(uid, user, email, fullname, authorities).toJson.toString)
      .by("https://doktor.sssl.sch.bme.hu")
      .issuedAt(now)
      .expiresAt(now + jwtConf.expirationSecs)

    Jwt.encode(
      claim,
      jwtConf.privateKey,
      JwtAlgorithm.RS256,
    )
  }

  def validateAndDecode(token: String): Option[JwtPayload] =
    JwtSprayJson
      .decodeJson(token, jwtConf.publicKey, Seq(JwtAlgorithm.RS256))
      .map(_.convertTo[JwtPayload])
      .toOption
}

object JwtService {
  import DefaultJsonProtocol._

  case class JwtPayload(
      uid: String,
      user: String,
      email: String,
      fullname: String,
      authorities: Seq[Authorities],
  )

  implicit
  val jwtPayloadFormatter: RootJsonFormat[JwtPayload] = jsonFormat5(JwtPayload)
}
