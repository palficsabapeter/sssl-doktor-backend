package hu.bme.sch.sssl.doktor.auth

import hu.bme.sch.sssl.doktor.`enum`.Authorities
import hu.bme.sch.sssl.doktor.app.Config
import hu.bme.sch.sssl.doktor.auth.JwtService.JwtPayload
import hu.bme.sch.sssl.doktor.testutil.TestBase
import hu.bme.sch.sssl.doktor.util.ErrorUtil.AuthError

class JwtAuthSpec extends TestBase {
  trait TestScope {
    val config: Config                   = new Config {}
    implicit val jwtConf: Config.JwtConf = config.jwtConf

    implicit val service: JwtService = mock[JwtService]

    val jwtAuth: JwtAuth = new JwtAuth()
  }

  "JwtAuth" should {
    "#auth" should {
      "return a JwtPayload if validation and decode was successful" in new TestScope {
        private val payload = JwtPayload(
          "userId1",
          "user1",
          "user1@mail.com",
          "User1 User1",
          Seq(Authorities.Admin, Authorities.Clerk, Authorities.User),
        )
        when(service.validateAndDecode(any[String])).thenReturn(Some(payload))

        await(jwtAuth.auth("valid.token").value) shouldBe Right(payload)

        verify(service, times(1)).validateAndDecode("valid.token")
      }

      "return an AuthError if validation or decode was unsuccessful" in new TestScope {
        private val error = AuthError("Invalid JWT token!")
        when(service.validateAndDecode(any[String])).thenReturn(None)

        await(jwtAuth.auth("in.valid.token").value) shouldBe Left(error)

        verify(service, times(1)).validateAndDecode("in.valid.token")
      }
    }
  }
}
