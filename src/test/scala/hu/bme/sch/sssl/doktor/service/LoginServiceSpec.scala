package hu.bme.sch.sssl.doktor.service

import cats.implicits._
import hu.bme.sch.sssl.doktor.`enum`.Authorities
import hu.bme.sch.sssl.doktor.app.Config
import hu.bme.sch.sssl.doktor.auth.JwtService.JwtPayload
import hu.bme.sch.sssl.doktor.repository.AuthRepository
import hu.bme.sch.sssl.doktor.repository.AuthRepository.UserAuthDbo
import hu.bme.sch.sssl.doktor.service.LoginService.{OrgMembership, ProfileResponse}
import hu.bme.sch.sssl.doktor.testutil.{AuthTestUtil, TestBase}
import hu.bme.sch.sssl.doktor.util.ErrorUtil.AuthError

class LoginServiceSpec extends TestBase with AuthTestUtil {
  trait TestScope {
    val config: Config                           = new Config {}
    implicit val schAuthConf: Config.SchAuthConf = config.schAuthConf

    implicit val repo: AuthRepository = mock[AuthRepository]

    val service: LoginService = new LoginService()

    val profile: ProfileResponse = ProfileResponse(
      "userId1",
      "User",
      "User",
      "user@mail.com",
      Seq(OrgMembership(schAuthConf.memberOf, "körtag")),
    )

    val expectedPayload: JwtPayload = JwtPayload(
      profile.internal_id,
      s"${profile.sn} ${profile.givenName}",
      profile.mail,
      Seq(Authorities.User),
    )
  }

  "LoginService" should {
    "#validateAndEncode" should {
      "return an AuthError if user is not member of SSSL" in new TestScope {
        await(service.validateAndEncode(profile.copy(eduPersonEntitlement = Seq.empty[OrgMembership])).value) shouldBe
          Left(AuthError(s"Not a member of ${schAuthConf.memberOf}!"))

        verify(repo, times(0)).findById(any[String])
      }

      "return JwtResponse with only User authority" in new TestScope {
        whenF(repo.findById(any[String])).thenReturn(None)

        await(service.validateAndEncode(profile).value).map { res =>
          val payload = jwtService.validateAndDecode(res.jwt)
          payload shouldBe a[Some[_]]
          payload.get shouldBe expectedPayload
        }

        verify(repo, times(1)).findById(profile.internal_id)
      }

      "return a JwtResponse with User and Clerk authorities" in new TestScope {
        val dbo: UserAuthDbo = UserAuthDbo(profile.internal_id, Seq(Authorities.Clerk))
        whenF(repo.findById(any[String])).thenReturn(Some(dbo))

        await(service.validateAndEncode(profile).value).map { res =>
          val payload = jwtService.validateAndDecode(res.jwt)
          payload shouldBe a[Some[_]]
          payload.get shouldBe expectedPayload.copy(authorities = Seq(Authorities.User, Authorities.Clerk))
        }

        verify(repo, times(1)).findById(profile.internal_id)
      }

      "return a JwtResponse with User, Clerk and Admin authorities" in new TestScope {
        val dbo: UserAuthDbo = UserAuthDbo(profile.internal_id, Seq(Authorities.Clerk, Authorities.Admin))
        whenF(repo.findById(any[String])).thenReturn(Some(dbo))

        await(service.validateAndEncode(profile).value).map { res =>
          val payload = jwtService.validateAndDecode(res.jwt)
          payload shouldBe a[Some[_]]
          payload.get shouldBe expectedPayload.copy(authorities = Seq(Authorities.User, Authorities.Clerk, Authorities.Admin))
        }

        verify(repo, times(1)).findById(profile.internal_id)
      }
    }
  }
}
