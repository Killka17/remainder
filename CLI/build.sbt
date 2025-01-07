import scala.collection.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.4"

lazy val root = (project in file("."))
  .settings(
    name := "CLI"
  )

libraryDependencies ++= Seq(


  // STTP Client - HTTP-клиент
  "com.softwaremill.sttp.client3" %% "core" % "3.8.15",

  "com.softwaremill.sttp.client3" %% "circe" % "3.8.15",

  // Circe - работа с JSON
  "io.circe" %% "circe-core" % "0.14.5",
  "io.circe" %% "circe-generic" % "0.14.5",

  // Tapir - создание REST API
  "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.11.10",

)