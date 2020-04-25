package hu.bme.sch.sssl.doktor.app

import pureconfig.{generic, ConfigSource}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

class Config {
  import Config._
  import ConfigSource.default.at
  import generic.auto._

  implicit val migratorConf: MigratorConf          = at("postgre.db").loadOrThrow[MigratorConf]
  implicit val dbConf: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("postgre")
}

object Config {
  case class MigratorConf(
      url: String,
      user: String,
      password: String,
  )
}
