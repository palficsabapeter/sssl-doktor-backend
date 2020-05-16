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
          init   <- db.run(repo.authorities.result)
          insert <- repo.upsert(dbo)
          found  <- db.run(repo.authorities.result)
        } yield {
          init.size shouldBe 0
          insert shouldBe 1
          found.size shouldBe 1
          found.head shouldBe dbo.copy(id = found.head.id)
        })
      }

      "update existing row" in new TestScope {
        private val newDbo = dbo.copy(authorities = Seq(Authorities.Clerk, Authorities.Admin))

        await(for {
          init          <- db.run(repo.authorities.result)
          insert        <- repo.upsert(dbo)
          insertedFound <- db.run(repo.authorities.result)
          update        <- repo.upsert(newDbo)
          updatedFound  <- db.run(repo.authorities.result)
        } yield {
          init.size shouldBe 0
          insert shouldBe 1
          insertedFound.size shouldBe 1
          insertedFound.head shouldBe dbo.copy(id = insertedFound.head.id)
          update shouldBe 1
          updatedFound.size shouldBe 1
          updatedFound.head shouldBe newDbo.copy(id = updatedFound.head.id)
        })
      }
    }

    "#findById" should {
      "return Some(UserAuthDbo) for found uid" in new TestScope {
        await(for {
          init   <- db.run(repo.authorities.result)
          insert <- repo.upsert(dbo)
          found  <- repo.findById(dbo.uid)
        } yield {
          init.size shouldBe 0
          insert shouldBe 1
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
