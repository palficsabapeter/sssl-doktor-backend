package hu.bme.sch.sssl.doktor.service

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import akka.http.scaladsl.{Http, HttpExt}
import cats.data.EitherT
import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.Authorities
import hu.bme.sch.sssl.doktor.app.Config.SchAuthConf
import hu.bme.sch.sssl.doktor.auth.JwtService
import hu.bme.sch.sssl.doktor.auth.JwtService.JwtPayload
import hu.bme.sch.sssl.doktor.repository.AuthRepository
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AppErrorOr, AuthError}
import hu.bme.sch.sssl.doktor.util.TapirEndpointUtil
import io.circe.{Decoder, Encoder}

import scala.concurrent.{ExecutionContext, Future}

class LoginService(
    implicit
    schAuthConf: SchAuthConf,
    jwtService: JwtService,
    authRepo: AuthRepository,
    ec: ExecutionContext,
    as: ActorSystem,
) {
  import LoginService._

  val httpExt: HttpExt = Http()

  def login(authorizationCode: String): AppErrorOr[JwtResponse] =
    for {
      tokens  <- getAccessToken(authorizationCode)
      profile <- getProfile(tokens.access_token)
      jwt     <- validateAndEncode(profile)
    } yield jwt

  private[service] def getAccessToken(authorizationCode: String): AppErrorOr[TokenResponse] = {
    val formBody = Query(
      "code"       -> authorizationCode,
      "grant_type" -> "authorization_code",
    )

    val req = HttpRequest(
      uri = Uri(schAuthConf.tokenEndpoint),
      headers = Authorization(BasicHttpCredentials(schAuthConf.clientId, schAuthConf.clientSecret)) :: Nil,
      entity = FormData(formBody).toEntity,
      method = HttpMethods.POST,
    )

    for {
      res   <- EitherT.right(httpExt.singleRequest(req))
      token <- EitherT.right(Unmarshal(res).to[TokenResponse])
    } yield token
  }

  private[service] def getProfile(accessToken: String): AppErrorOr[ProfileResponse] = {
    val req = HttpRequest(uri = Uri(schAuthConf.profileEndpoint).withQuery(Query("access_token" -> accessToken)))

    for {
      res     <- EitherT.right(httpExt.singleRequest(req))
      profile <- EitherT.right(Unmarshal(res).to[ProfileResponse])
    } yield profile
  }

  private[service] def validateAndEncode(profile: ProfileResponse): AppErrorOr[JwtResponse] = {
    def org: Option[OrgMembership] =
      profile.eduPersonEntitlement
        .find(_.name.equals(schAuthConf.memberOf))

    def fetchDbAuthsAndEncode: Future[JwtResponse] =
      authRepo
        .findById(profile.internal_id)
        .map { useAuthOpt =>
          val auths = Authorities.User :: useAuthOpt.map(_.authorities.toList).getOrElse(Nil)
          val payload = JwtPayload(
            profile.internal_id,
            s"${profile.sn} ${profile.givenName}",
            profile.mail,
            auths,
          )

          JwtResponse(jwtService.encode(payload))
        }

    for {
      _   <- EitherT.fromOption[Future](org, AuthError(s"Not a member of ${schAuthConf.memberOf}!"))
      res <- EitherT.right(fetchDbAuthsAndEncode)
    } yield res
  }
}

object LoginService {
  import io.circe.generic.semiauto._

  case class TokenResponse(
      access_token: String,
  )

  case class OrgMembership(
      name: String,
      status: String,
  )

  case class ProfileResponse(
      internal_id: String,
      sn: String,
      givenName: String,
      mail: String,
      eduPersonEntitlement: Seq[OrgMembership],
  )

  case class JwtResponse(
      jwt: String,
  )

  implicit
  val tokenResponseDecoder: Decoder[TokenResponse]                              = deriveDecoder
  implicit val tokenResponseUnmarshaller: FromEntityUnmarshaller[TokenResponse] = TapirEndpointUtil.unmarshaller[TokenResponse]

  implicit val orgMembershipDecoder: Decoder[OrgMembership]                         = deriveDecoder
  implicit val profileResponseDecoder: Decoder[ProfileResponse]                     = deriveDecoder
  implicit val profileResponseUnmarshaller: FromEntityUnmarshaller[ProfileResponse] = TapirEndpointUtil.unmarshaller[ProfileResponse]

  implicit val jwtResponseEncoder: Encoder[JwtResponse] = deriveEncoder
}
