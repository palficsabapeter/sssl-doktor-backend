package hu.bme.sch.sssl.doktor.testutil

import hu.bme.sch.sssl.doktor.app.{Config, Migrator}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

trait RepositoryTestBase extends TestBase with BeforeAndAfterAll with BeforeAndAfterEach {
  implicit val config: Config = new Config {}
  implicit val db: Database   = config.dbConf.db

  def cleanDb(): Future[_]

  override def beforeAll(): Unit =
    await(for {
      _ <- Migrator.run(config.migratorConf)
    } yield ())

  override def beforeEach(): Unit =
    await(for {
      _ <- cleanDb()
    } yield ())

  override def afterAll(): Unit =
    await(for {
      _ <- cleanDb()
    } yield ())
}
