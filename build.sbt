import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings

name := "sssl-doktor-backend"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= {
  val akkaHttpV = "10.1.11"
  val akkaV     = "2.6.4"
  val jwtV      = "4.3.0"
  val mockitoV  = "1.13.1"

  Seq(
    //akka
    "com.typesafe.akka" %% "akka-http"            % akkaHttpV,
    "com.typesafe.akka" %% "akka-slf4j"           % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "com.typesafe.akka" %% "akka-stream"          % akkaV,
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
    "com.typesafe.akka" %% "akka-http-testkit"  % akkaHttpV % Test,
    "com.typesafe.akka" %% "akka-testkit"       % akkaV     % Test,
    "org.mockito"       %% "mockito-scala"      % mockitoV  % Test,
    "org.mockito"       %% "mockito-scala-cats" % mockitoV  % Test,
    "org.scalatest"     %% "scalatest"          % "3.1.1"   % Test,
  )
}

enablePlugins(JavaAppPackaging)
enablePlugins(BuildInfoPlugin)

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
