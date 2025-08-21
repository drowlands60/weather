import Dependencies._

ThisBuild / scalaVersion     := "2.13.14"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "weather",
    libraryDependencies += munit % Test
  )

  
libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.4",
  "org.http4s" %% "http4s-dsl" % "0.23.27",
  "org.http4s" %% "http4s-ember-server" % "0.23.27",
  "org.http4s" %% "http4s-ember-client" % "0.23.27",
  "org.http4s" %% "http4s-circe" % "0.23.27",
  "io.circe" %% "circe-core" % "0.14.10",
  "io.circe" %% "circe-generic" % "0.14.10",
  "io.circe" %% "circe-parser" % "0.14.10",
  "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
  "ch.qos.logback" % "logback-classic" % "1.5.6" 
)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
