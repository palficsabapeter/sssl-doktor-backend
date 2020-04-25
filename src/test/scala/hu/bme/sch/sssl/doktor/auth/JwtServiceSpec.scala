package hu.bme.sch.sssl.doktor.auth

import akka.http.scaladsl.model.DateTime
import hu.bme.sch.sssl.doktor.`enum`.Authorities
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

    val uid         = "userId1"
    val user        = "user1"
    val email       = "user1@mail.com"
    val fullname    = "User1 User1"
    val authorities = Seq(Authorities.Admin, Authorities.Clerk, Authorities.User)
  }

  "JwtService" should {
    "#encode" should {
      "encode a valid JWT with expiration" in new TestScope {
        val epochCreated: Long = DateTime(2020, 4, 25, 0, 0, 0).clicks / 1000
        when(tp.epochSecs).thenReturn(epochCreated)

        val jwt =
          "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2Rva3Rvci5zc3NsLnNjaC5ibWUuaHUiLCJleHAiOjE1ODc4NTkyMDAsImlhdCI6MTU4Nzc3MjgwMCwiYXV0aG9yaXRpZXMiOlsiQURNSU4iLCJDTEVSSyIsIlVTRVIiXSwiZW1haWwiOiJ1c2VyMUBtYWlsLmNvbSIsImZ1bGxuYW1lIjoiVXNlcjEgVXNlcjEiLCJ1aWQiOiJ1c2VySWQxIiwidXNlciI6InVzZXIxIn0.Xf1iEle5kc3f6kBH6VFrY6yILX_YFpnYHd8k_O1rSGvLzd9fNvJ4JjdolYY9N_r9ftyZkrophKPSb1te9pcrrRfTi8GcToBQPt4DG87ALMcJKw8aExEUObZBiKikGrfeWK-05iIyh0DONTpWu4eHyI2XEa7d83VuGyh7lCsvZymr7x7xUbgAmEle5aoQnOKVUFBFQtg5RnxkFV9pQbi2u6eILYBm1NTNUH3El_haVOfFEQq6QWvgaK-BMy9TbEy-gXyQIBYc_4kXQuOT5_FR2ePsTxG5DtJl07Ov-ybQPfya6D_hgDGOUmIBbGULrvWMT7K8fn3fte4k-4mo8zD89Q"
        jwtService.encode(uid, user, email, fullname, authorities) shouldBe jwt
      }
    }

    "#validateAndDecode" should {
      "validate a valid jwt" in new TestScope {
        when(tp.epochSecs).thenReturn(System.currentTimeMillis() / 1000)
        val validJwt: String = jwtService.encode(uid, user, email, fullname, authorities)

        val payload: JwtPayload =
          JwtPayload(
            uid,
            user,
            email,
            fullname,
            authorities,
          )

        jwtService.validateAndDecode(validJwt) shouldBe Some(payload)
      }

      "NOT validate an expired jwt" in new TestScope {
        val epochCreated: Long = System.currentTimeMillis / 1000 - jwtConf.expirationSecs - 10
        when(tp.epochSecs).thenReturn(epochCreated)
        val expiredJwt: String = jwtService.encode(uid, user, email, fullname, authorities)

        jwtService.validateAndDecode(expiredJwt) shouldBe None
      }

      "NOT validate an invalid jwt" in new TestScope {
        val invalidJwt: String = "in.valid.jwt"

        jwtService.validateAndDecode(invalidJwt) shouldBe None
      }
    }
  }
}
