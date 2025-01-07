package remainder

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits.*
import doobie.Transactor
import org.http4s.{Header, HttpRoutes, Uri}
import org.http4s.client.dsl.io.*
import org.http4s.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.Logger
import remainder.controller.endpoints
import remainder.dbСlient.Impl
import remainder.service.RemainderStorage
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s.{Host, Port}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.{Executors, TimeUnit}

val xa = Transactor.fromDriverManager[IO](
  driver = "org.postgresql.Driver",
  url = s"jdbc:postgresql://db:${config.dbPort}/${config.dbName}",
  logHandler = None,
  user = config.dbUser,
  password = config.dbPassword
)

val client = new Impl
val myStorage: RemainderStorage = RemainderStorage.make(client, xa)

object ServerApp extends IOApp {

  private val findRemainderByDateUrl: Uri =
    Uri.unsafeFromString(s"http://${config.serverHost}:${config.serverPort}/api/v1/remainder")
  private val telegramApiUrl: Uri = Uri.unsafeFromString(
    s"https://api.telegram.org/bot8087449989:AAHULcYMmUzGCe9GXkt5Rkxer_zJpFm0_r0/sendMessage"
  )

  private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00")

  private def sendToTelegram(client: org.http4s.client.Client[IO], message: String): IO[Unit] = {
    val request = POST(
      telegramApiUrl.withQueryParam("chat_id", config.chatId).withQueryParam("text", message)
    )
    client.expect[String](request).attempt.flatMap {
      case Right(success) => IO.println(s"Telegram notification sent: $success")
      case Left(error)    => IO.println(s"Failed to send Telegram notification: $error")
    }
  }

  private def sendPostRequest(client: org.http4s.client.Client[IO]): IO[Unit] = {
    val currentTime = LocalDateTime.now().format(dateTimeFormatter)
    val request = GET(findRemainderByDateUrl.withQueryParam("date", currentTime))
      .putHeaders(Header("Accept", "application/json"))

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
    val scheduler = Executors.newScheduledThreadPool(1)
    IO {
      scheduler.scheduleAtFixedRate(
        () => sendPostRequest(client).unsafeRunSync(),
        0,
        1,
        TimeUnit.MINUTES
      )
    }.as(IO.unit)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val endpointsInstance = new endpoints(myStorage)

    val allRoutes = Http4sServerInterpreter[IO]().toRoutes(
      List(
        endpointsInstance.findRemainderByDateWithLogic,
        endpointsInstance.insertRemainderWithLogic,
        endpointsInstance.removeRemainderWithLogic,
        endpointsInstance.allRemaindersWithLogic
      )
    )

    val swaggerEndpoints = Http4sServerInterpreter[IO]().toRoutes(
      SwaggerInterpreter().fromEndpoints[IO](
        List(
          endpointsInstance.findRemainderByDate,
          endpointsInstance.insertRemainder,
          endpointsInstance.removeRemainder,
          endpointsInstance.allRemainders
        ),
        "Remainder",
        "0.1"
      )
    )

    val httpApp = Router("/" -> (allRoutes <+> swaggerEndpoints)).orNotFound

    val finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        EmberServerBuilder
          .default[IO]
          .withHost(Host.fromString(s"${config.serverHost}").get)
          .withPort(Port.fromInt(config.serverPort.toInt).get)
          .withHttpApp(finalHttpApp)
          .build
          .use { _ =>
            IO.println(s"Server started at ${config.serverHost}:${config.serverPort}") *> scheduleRemainderNotification(
              client
            ) *> IO.never
          }
      }
      .as(ExitCode.Success)
  }
}
