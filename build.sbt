import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings

name := "sssl-doktor-backend"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= {
  val akkaHttpV = "10.1.11"
  val akkaV     = "2.6.4"
  val slickV    = "3.3.2"
  val jwtV      = "4.3.0"
  val mockitoV  = "1.13.1"
  val tapirV    = "0.12.24"
  val circeV    = "0.12.0"

  Seq(
    //akka
    "com.typesafe.akka" %% "akka-http"            % akkaHttpV,
    "com.typesafe.akka" %% "akka-slf4j"           % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "com.typesafe.akka" %% "akka-stream"          % akkaV,
    //db
    "com.typesafe.slick" %% "slick"          % slickV,
    "com.typesafe.slick" %% "slick-hikaricp" % slickV,
    "org.flywaydb"        % "flyway-core"    % "6.4.0",
    "org.postgresql"      % "postgresql"     % "42.2.12",
    //jwt
    "com.pauldijou" %% "jwt-core"       % jwtV,
    "com.pauldijou" %% "jwt-spray-json" % jwtV,
    //logging
    "ch.qos.logback"       % "logback-classic"          % "1.2.3",
    "net.logstash.logback" % "logstash-logback-encoder" % "6.3",
    "org.codehaus.janino"  % "janino"                   % "3.1.2",
    "org.slf4j"            % "jul-to-slf4j"             % "1.7.30",
    //misc
    "io.spray"              %% "spray-json"       % "1.3.5",
    "com.github.pureconfig" %% "pureconfig"       % "0.12.3",
    "org.typelevel"         %% "cats-core"        % "2.1.1",
    "ch.megard"             %% "akka-http-cors"   % "0.4.2",
    "org.scalameta"         %% "scalafmt-dynamic" % "2.5.0-RC1",
    //test
    "com.typesafe.akka"     %% "akka-http-testkit"  % akkaHttpV % Test,
    "com.typesafe.akka"     %% "akka-testkit"       % akkaV     % Test,
    "org.mockito"           %% "mockito-scala"      % mockitoV  % Test,
    "org.mockito"           %% "mockito-scala-cats" % mockitoV  % Test,
    "org.scalatest"         %% "scalatest"          % "3.1.1"   % Test,
    "com.github.tomakehurst" % "wiremock"           % "2.25.1"  % Test,
    //tapir
    "com.softwaremill.sttp.tapir" %% "tapir-core"                 % tapirV,
    "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server"     % tapirV,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe"           % tapirV,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"         % tapirV,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml"   % tapirV,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-akka-http" % tapirV,
    //circe
    "io.circe" %% "circe-yaml"    % circeV,
    "io.circe" %% "circe-generic" % circeV,
    "io.circe" %% "circe-parser"  % circeV,
  )
}

enablePlugins(JavaAppPackaging)
enablePlugins(BuildInfoPlugin)
enablePlugins(FlywayPlugin)

lazy val root = (project in file("."))
  .configs(ItTest)
  .settings(inConfig(ItTest)(itTestSettings): _*)

lazy val ItTest         = config("it") extend Test
lazy val itTestSettings = Defaults.itSettings ++ scalafmtConfigSettings
concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

addCommandAlias("check", "fmtCheck test it:test stage")
addCommandAlias(
  "fmtCheck",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck it:scalafmtCheck",
)
addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt it:scalafmt")

lazy val buildTime                       = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
lazy val builtAtMillis: SettingKey[Long] = SettingKey[Long]("builtAtMillis", "time of build")
ThisBuild / builtAtMillis := buildTime.toInstant.toEpochMilli
lazy val builtAtString: SettingKey[String] = SettingKey[String]("builtAtString", "time of build")
ThisBuild / builtAtString := buildTime.toString

buildInfoKeys := Seq[BuildInfoKey](
  name,
  version,
  scalaVersion,
  sbtVersion,
  BuildInfoKey.action("commitHash") {
    git.gitHeadCommit.value
  },
  builtAtString,
  builtAtMillis,
)
buildInfoPackage := "hu.bme.sch.sssl.doktor"

version := git.gitHeadCommit.value.getOrElse("no_info")
