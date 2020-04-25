package hu.bme.sch.sssl.doktor.app

import pureconfig.generic.ProductHint
import pureconfig.{generic, CamelCase, ConfigFieldMapping, ConfigSource}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

class Config {
  import Config._
  import ConfigSource.default.at
  import generic.auto._

  implicit def hint[T]: ProductHint[T]             = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
  implicit val migratorConf: MigratorConf          = at("postgre.db").loadOrThrow[MigratorConf]
  implicit val dbConf: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("postgre")
  implicit val jwtConf: JwtConf                    = at("jwt").loadOrThrow[JwtConf]
}

object Config {
  case class MigratorConf(
      url: String,
      user: String,
      password: String,
  )

  case class JwtConf(
      privateKey: String,
      publicKey: String,
      expirationSecs: Long,
  )
}
