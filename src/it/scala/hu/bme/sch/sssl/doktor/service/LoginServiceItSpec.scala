package hu.bme.sch.sssl.doktor.service

import java.net.URI

import com.github.tomakehurst.wiremock.client.WireMock._
import hu.bme.sch.sssl.doktor.`enum`.Authorities
import hu.bme.sch.sssl.doktor.app.Config
import hu.bme.sch.sssl.doktor.auth.JwtService.JwtPayload
import hu.bme.sch.sssl.doktor.repository.AuthRepository
import hu.bme.sch.sssl.doktor.repository.AuthRepository.UserAuthDbo
import hu.bme.sch.sssl.doktor.service.LoginService.{OrgMembership, ProfileResponse, TokenResponse}
import hu.bme.sch.sssl.doktor.testutil.{AuthTestUtil, WireMockTestBase}

import scala.concurrent.Future

class LoginServiceItSpec extends WireMockTestBase with AuthTestUtil {
  val uri              = new URI("http://localhost:9000")
  val hostName: String = uri.getHost
  val port: Int        = uri.getPort

  trait TestScope {
    val config: Config                           = new Config {}
    implicit val schAuthConf: Config.SchAuthConf = config.schAuthConf

    implicit val repo: AuthRepository = mock[AuthRepository]

    val service: LoginService = new LoginService()

    private val tokenRes =
      s"""{
         |  "access_token": "access_token",
         |  "expires_in": 3600,
         |  "token_type": "Bearer",
         |  "scope": "basic sn givenName mail eduPersonEntitlement",
         |  "refresh_token": "refresh_token"
         |}""".stripMargin

    mockServer.stubFor(
      post(urlEqualTo("/oauth2/token"))
        .withBasicAuth(schAuthConf.clientId, schAuthConf.clientSecret)
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(tokenRes)
            .withStatus(200),
        ),
    )

    private val profileRes =
      s"""{
         |    "internal_id": "userId1",
         |    "sn": "User1",
         |    "givenName": "User1",
         |    "mail": "user1@mail.com",
         |    "eduPersonEntitlement": [
         |        {
         |            "id": 18,
         |            "name": "Szent Schönherz Senior Lovagrend",
         |            "title": [
         |                "volt körvezető"
         |            ],
         |            "status": "tag",
         |            "start": "2016-12-12",
         |            "end": null
         |        }
         |    ]
         |}""".stripMargin

    mockServer.stubFor(
      get(urlEqualTo("/api/profile?access_token=access_token"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "text/json")
            .withBody(profileRes)
            .withStatus(200),
        ),
    )
  }

  "LoginService" should {
    "#login" should {
      "login user" in new TestScope {
        private val expectedPayload = JwtPayload(
          "userId1",
          "User1 User1",
          "user1@mail.com",
          Seq(Authorities.User, Authorities.Clerk, Authorities.Admin),
        )

        private val dbo = UserAuthDbo("userId1", Seq(Authorities.Clerk, Authorities.Admin))
        when(repo.findById(any[String])).thenReturn(Future.successful(Some(dbo)))

        await(service.login("auth_code").value)
          .map(res => jwtService.validateAndDecode(res.jwt) shouldBe Some(expectedPayload))

        verify(repo, times(1)).findById("userId1")
      }
    }

    "#getAccessToken" should {
      s"send POST to /oauth2/token" should {
        "fetch access token" in new TestScope {
          private val res = TokenResponse("access_token")

          await(service.getAccessToken("auth_code").value) shouldBe Right(res)

          mockServer.verify(postRequestedFor(urlEqualTo("/oauth2/token")))
        }
      }
    }

    "#getProfile" should {
      "send GET to /api/profile?access_token={access_token}" should {
        "fetch user's profile" in new TestScope {
          private val res = ProfileResponse(
            "userId1",
            "User1",
            "User1",
            "user1@mail.com",
            Seq(OrgMembership("Szent Schönherz Senior Lovagrend", "tag")),
          )

          await(service.getProfile("access_token").value) shouldBe Right(res)

          mockServer.verify(getRequestedFor(urlEqualTo("/api/profile?access_token=access_token")))
        }
      }
    }
  }
}
