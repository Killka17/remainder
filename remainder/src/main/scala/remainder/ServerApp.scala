package remainder

import cats.effect.{Clock, ExitCode, IO, IOApp, Resource}
import cats.implicits.*
import doobie.Transactor
import org.http4s.{Header, HttpRoutes, MediaType, Uri}
import org.http4s.client.dsl.io.*
import org.http4s.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.Logger
import remainder.controller.{EndpointsWithLogic, endpoints}
import remainder.dbclient.Impl
import remainder.service.RemainderStorage
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s.{Host, Port}
import org.http4s.headers.Accept
import scala.concurrent.duration.DurationInt
import doobie.implicits.toConnectionIOOps

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.{Executors, TimeUnit}

object ServerApp extends IOApp {

  private val findRemainderByDateUrl: Uri =
    Uri.unsafeFromString(s"http://${serverConfig.serverHost}:${serverConfig.serverPort}/api/v1/remainder")
  private val telegramApiUrl: Uri = Uri.unsafeFromString(
    serverConfig.telegramURL
  )

  private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00")

  private def sendToTelegram(client: org.http4s.client.Client[IO], message: String): IO[Unit] = {
    val request = POST(
      telegramApiUrl.withQueryParam("chat_id", serverConfig.chatId).withQueryParam("text", message)
    )
    client.expect[String](request).attempt.flatMap {
      case Right(success) => IO.println(s"Telegram notification sent: $success")
      case Left(error)    => IO.println(s"Failed to send Telegram notification: $error")
    }
  }

  private def sendPostRequest(client: org.http4s.client.Client[IO]): IO[Unit] = {
    val currentTime = LocalDateTime.now().format(dateTimeFormatter)
    val request = GET(findRemainderByDateUrl.withQueryParam("date", currentTime))
      .putHeaders(Accept(MediaType("application", "json")))

    client.expect[String](request).attempt.flatMap {
      case Right(notes) if notes != "[]" =>
        val cleanedNotes =
          notes.stripPrefix("[").stripSuffix("]").split(",").map(_.trim.stripPrefix("\"").stripSuffix("\""))
        cleanedNotes.toList.traverse_(sendToTelegram(client, _))
      case Right(_) =>
        IO.println("No notes found.")
      case Left(error) =>
        IO.println(s"Request to findRemainderByDate failed: $error")
    }
  }

  private def scheduleRemainderNotification(client: org.http4s.client.Client[IO]): IO[Unit] = {
    def loop: IO[Unit] =
      sendPostRequest(client) >>
        Clock[IO].sleep(1.minute) >>
        loop

    loop.start.void
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val xa = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = s"jdbc:postgresql://db:${dbConfig.dbPort}/${dbConfig.dbName}",
      logHandler = None,
      user = dbConfig.dbUser,
      password = dbConfig.dbPassword
    )

    val sqlClient = new Impl
    val myStorage: RemainderStorage = RemainderStorage.make(sqlClient, xa)
    val endpointsInstance = new EndpointsWithLogic(myStorage)

    val allRoutes = Http4sServerInterpreter[IO]().toRoutes(
      endpointsInstance.all
    )

    val swaggerEndpoints = Http4sServerInterpreter[IO]().toRoutes(
      SwaggerInterpreter().fromServerEndpoints[IO](
        endpointsInstance.all,
        "Remainder",
        "0.1"
      )
    )

    val httpApp = Router("/" -> (allRoutes <+> swaggerEndpoints)).orNotFound

    val finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

    (for {
      client <- EmberClientBuilder
        .default[IO]
        .build
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString(s"${serverConfig.serverHost}").get)
        .withPort(Port.fromInt(serverConfig.serverPort.toInt).get)
        .withHttpApp(finalHttpApp)
        .build
    } yield client)
      .use { client =>
        sqlClient.createTableIfNotExists.transact(xa) *>
          IO.println(
            s"Server started at ${serverConfig.serverHost}:${serverConfig.serverPort}"
          ) *> scheduleRemainderNotification(
            client
          ) *> IO.never
      }
      .as(ExitCode.Success)
  }
}
