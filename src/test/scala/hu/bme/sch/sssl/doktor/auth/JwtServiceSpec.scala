package hu.bme.sch.sssl.doktor.auth

import akka.http.scaladsl.model.DateTime
import hu.bme.sch.sssl.doktor.`enum`.Authorities
import hu.bme.sch.sssl.doktor.`enum`.Authorities.Authorities
import hu.bme.sch.sssl.doktor.app.Config
import hu.bme.sch.sssl.doktor.auth.JwtService.JwtPayload
import hu.bme.sch.sssl.doktor.testutil.TestBase
import hu.bme.sch.sssl.doktor.util.TimeProvider

class JwtServiceSpec extends TestBase {
  trait TestScope {
    val config: Config                   = new Config {}
    implicit val jwtConf: Config.JwtConf = config.jwtConf

    implicit val tp: TimeProvider = mock[TimeProvider]
    val jwtService: JwtService    = new JwtService()

    val uid: String                   = "userId1"
    val user: String                  = "user1"
    val email: String                 = "user1@mail.com"
    val authorities: Seq[Authorities] = Seq(Authorities.Admin, Authorities.Clerk, Authorities.User)
    val payload: JwtPayload           = JwtPayload(uid, user, email, authorities)
  }

  "JwtService" should {
    "#encode" should {
      "encode a valid JWT with expiration" in new TestScope {
        private val epochCreated: Long = DateTime(2020, 4, 25, 0, 0, 0).clicks / 1000
        when(tp.epochSecs).thenReturn(epochCreated)

        private val jwt =
          "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2Rva3Rvci5zc3NsLnNjaC5ibWUuaHUiLCJleHAiOjE1ODc4NTkyMDAsImlhdCI6MTU4Nzc3MjgwMCwiYXV0aG9yaXRpZXMiOlsiQURNSU4iLCJDTEVSSyIsIlVTRVIiXSwiZW1haWwiOiJ1c2VyMUBtYWlsLmNvbSIsInVpZCI6InVzZXJJZDEiLCJ1c2VyIjoidXNlcjEifQ.lQE8Ix8cEgGoFaGV1sAQvBPaA9M8B5QEbt9TCA1zkebS77CSf1WIkXEpT-HB-39Dz0XJGdAH7BmVPduRhjFcLLWInIobYUq8TXXS4sUnPPDX0WUONNBDj9RVJ2wVHhUkNtu8HY3jpzWEiCYIum7QuAT3BzG8Hiu12Y2UOGKH-v5qwMvkxatsnZ0PiXvGrAFuTzKxHIHwDeDkGkg81Z6_V79vRl5_Lexk3szBZUKKG-RPLnzItWnRzYnChB8dLnIFxOyjpEZyiRlKDr88G0djh6w64NlHllEl5RaJ4DA2502tdfRETeydUty4ZFGgFtV4M7b40cgG75oAO2bqGp7igA"
        jwtService.encode(payload) shouldBe jwt
      }
    }

    "#validateAndDecode" should {
      "validate a valid jwt" in new TestScope {
        when(tp.epochSecs).thenReturn(System.currentTimeMillis() / 1000)

        private val validJwt: String = jwtService.encode(payload)
        jwtService.validateAndDecode(validJwt) shouldBe Some(payload)
      }

      "NOT validate an expired jwt" in new TestScope {
        private val epochCreated: Long = System.currentTimeMillis / 1000 - jwtConf.expirationSecs - 10
        when(tp.epochSecs).thenReturn(epochCreated)

        private val expiredJwt: String = jwtService.encode(payload)
        jwtService.validateAndDecode(expiredJwt) shouldBe None
      }

      "NOT validate an invalid jwt" in new TestScope {
        private val invalidJwt: String = "in.valid.jwt"
        jwtService.validateAndDecode(invalidJwt) shouldBe None
      }
    }
  }
}
