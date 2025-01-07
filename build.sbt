import scala.collection.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.4"

lazy val cli = (project in file("CLI"))
  .settings(
    name := "CLI",
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
  )

lazy val remainder = (project in file("remainder"))
  .settings(
    name := "remainder",
    libraryDependencies ++= Seq(
      // Cats Effect - функциональное программирование и эффекты
      "org.typelevel" %% "cats-effect" % "3.5.2",

      // Doobie - работа с базами данных
      "org.tpolecat" %% "doobie-core"      % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-h2"        % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-hikari"    % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-postgres"  % "1.0.0-RC4",

      // STTP Client - HTTP-клиент
      "com.softwaremill.sttp.client3" %% "core" % "3.8.15",

      "com.softwaremill.sttp.client3" %% "circe" % "3.8.15",

      // Circe - работа с JSON
      "io.circe" %% "circe-core" % "0.14.5",
      "io.circe" %% "circe-generic" % "0.14.5",
      "io.circe" %% "circe-parser" % "0.14.5",

      // Tapir - создание REST API
      "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.11.10",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.11.10",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.11.10",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"    % "1.11.10",

      // Логирование
      "org.slf4j" % "slf4j-api" % "1.7.2",
      "ch.qos.logback" % "logback-classic" % "1.2.6",
      "tf.tofu"            %% "tofu-logging"   % "0.13.6",

      // Tofu - функциональные утилиты
      "tf.tofu" %% "tofu-core-ce3" % "0.13.6",

      // PureConfig - конфигурация
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.8",

      "org.http4s" %% "http4s-dsl" % "0.23.23",
      "org.http4s" %% "http4s-circe" % "0.23.23",
      "org.http4s" %% "http4s-client" % "0.23.23",
      "org.http4s" %% "http4s-server" % "0.23.23",
      "org.http4s" %% "http4s-ember-server" % "0.23.20",
      "org.http4s" %% "http4s-ember-client" % "0.23.20",
      "com.comcast" %% "ip4s-core" % "3.6.0"
    )
  )

lazy val root = (project in file("."))
  .aggregate(cli, remainder)
  .settings(
    name := "RootProject"
  )
