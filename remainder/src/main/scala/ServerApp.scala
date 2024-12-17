import cats.effect.IO
import db_client.Impl
import doobie.Transactor
import sttp.tapir.server.netty.NettyFutureServer
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.netty.{FutureRoute, NettyFutureServerInterpreter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import sttp.client3.*

import java.util.concurrent.{Executors, TimeUnit}
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import service.RemainderStorage

val xa = Transactor.fromDriverManager[IO](
  driver = "org.postgresql.Driver",
  url = s"jdbc:postgresql://${config.dbHost}:${config.dbPort}/${config.dbName}",
  logHandler = None,
  user = config.dbUser,
  password = config.dbPassword
)

val client = new Impl
val storage: RemainderStorage[IO] = RemainderStorage.make(client, xa)

object ServerApp extends App {

  private val findRemainderByDateUrl: String = "http://localhost:8080/api/v1/remainder"

  private val telegramApiUrl: String =
    "https://api.telegram.org/bot8087449989:AAHULcYMmUzGCe9GXkt5Rkxer_zJpFm0_r0/sendMessage"

  val backend: SttpBackend[Identity, Any] = HttpClientSyncBackend()

  private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00")

  private def sendToTelegram(message: String): Unit = {
    val request = basicRequest
      .post(uri"$telegramApiUrl?chat_id=${config.chatId}&text=$message")

    val response = request.send(backend)

    response.body match {
      case Right(success) => println(s"Telegram notification sent: $success")
      case Left(error)    => println(s"Failed to send Telegram notification: $error")
    }
  }

  def sendPostRequest(): Unit = {
    val currentTime = LocalDateTime.now().format(dateTimeFormatter)
    val request = basicRequest
      .get(uri"$findRemainderByDateUrl?date=$currentTime")

    val response = request.send(backend)

    response.body match {
      case Right(notes) if notes != "[]" =>
        val cleanedNotes = notes.stripPrefix("[").stripSuffix("]").split(",").map(_.trim.stripPrefix("\"").stripSuffix("\""))
        cleanedNotes.foreach { note =>
          sendToTelegram(note)
        }
      case Right(_) =>
        println("No notes found.")
      case Left(error) =>
        println(s"Request to findRemainderByDate failed: $error")
    }
  }

  private def scheduleRemainderNotification(): Unit = {
    val scheduler = Executors.newScheduledThreadPool(1)
    scheduler.scheduleAtFixedRate(
      () => sendPostRequest(),
      0,
      1,
      TimeUnit.MINUTES
    )
  }

  private val allRoutes = List(
    endpoints.findRemainderByDateWithLogic,
    endpoints.insertRemainderWithLogic,
    endpoints.removeRemainderWithLogic,
    endpoints.allRemaindersWithLogic
  )

  private val swaggerEndpoints = SwaggerInterpreter()
    .fromEndpoints[Future](
      List(endpoints.findRemainderByDate, endpoints.insertRemainder, endpoints.removeRemainder, endpoints.allRemainders),
      "Remainder",
      "0.1"
    )

  private val swaggerRoute: FutureRoute = NettyFutureServerInterpreter().toRoute(swaggerEndpoints)

  NettyFutureServer()
    .host(config.serverHost)
    .port(8080)
    .addEndpoints(allRoutes)
    .addRoute(swaggerRoute)
    .start()
    .map { binding =>
      println(s"Server started at localhost:${binding.port}")
      scheduleRemainderNotification()
    }
}
