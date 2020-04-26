package hu.bme.sch.sssl.doktor.repository

import hu.bme.sch.sssl.doktor.`enum`.Authorities
import hu.bme.sch.sssl.doktor.repository.AuthRepository.UserAuthDbo
import hu.bme.sch.sssl.doktor.testutil.RepositoryTestBase
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class AuthRepositorySpec extends RepositoryTestBase {
  override def cleanDb(): Future[_] = db.run(sqlu"""TRUNCATE user_auth""")

  trait TestScope {
    val repo: AuthRepository = new AuthRepository()

    val dbo: UserAuthDbo = UserAuthDbo("userId1", Seq(Authorities.Clerk))
  }

  "AuthRepository" should {
    "#upsert" should {
      "insert new row if there was no matching uid" in new TestScope {
        await(for {
          inserted <- repo.upsert(dbo)
          found    <- repo.findById(dbo.uid)
        } yield {
          inserted shouldBe 1
          found shouldBe a[Some[_]]
          found.get shouldBe dbo.copy(id = found.get.id)
        })
      }

      "update existing row" in new TestScope {
        private val newDbo = dbo.copy(authorities = Seq(Authorities.Clerk, Authorities.Admin))

        await(for {
          _       <- repo.upsert(dbo)
          updated <- repo.upsert(newDbo)
          found   <- repo.findById(newDbo.uid)
        } yield {
          updated shouldBe 1
          found shouldBe a[Some[_]]
          found.get shouldBe newDbo.copy(id = found.get.id)
        })
      }
    }

    "#findById" should {
      "return Some(UserAuthDbo) for found uid" in new TestScope {
        await(for {
          _     <- repo.upsert(dbo)
          found <- repo.findById(dbo.uid)
        } yield {
          found shouldBe a[Some[_]]
          found.get shouldBe dbo.copy(id = found.get.id)
        })
      }

      "return None for non existing uid" in new TestScope {
        await(repo.findById(dbo.uid)) shouldBe None
      }
    }
  }
}
