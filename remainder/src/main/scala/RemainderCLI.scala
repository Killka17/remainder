import domain.Remainder
import io.circe.{Decoder, Encoder}
import sttp.client3.*
import sttp.client3.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import sttp.tapir.Schema

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.io.StdIn
import domain.timestampDecoder
import domain.timestampEncoder


object RemainderCLI {

  private val serverUrl = uri"http://localhost:8080/api/v1/remainder"

  given timestampSchema: Schema[Timestamp] = Schema.string

  given remainderSchema: Schema[Remainder] = Schema.derived[Remainder]
  

  val backend = HttpClientSyncBackend()

  val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  private def parseTimestamp(input: String): Timestamp = {
    Timestamp.valueOf(LocalDateTime.parse(input, timestampFormatter))
  }

  private def createRemainder(): Unit = {
    println("Enter text:")
    val name = StdIn.readLine()

    println("Enter reminder date YYYY-MM-DD HH:MM")
    val dateInput = StdIn.readLine()

    try {
      val reminderDate = parseTimestamp(s"$dateInput:00")
      val remainder = Remainder(name, reminderDate)

      val request = basicRequest
        .post(serverUrl)
        .body(remainder.asJson)
        .response(asJson[Remainder])

      val response = request.send(backend)

      response.body match {
        case Right(createdRemainder) =>
          println(s"Remainder created successfully: $createdRemainder")
        case Left(error) =>
          println(s"Failed to create remainder: $error")
      }
    } catch {
      case e: Exception =>
        println(s"Invalid date format: ${e.getMessage}")
    }
  }

  private def deleteRemainder(): Unit = {
    println("Enter name of the remainder to delete:")
    val name = StdIn.readLine()

    println("Enter reminder date (YYYY-MM-DD HH:MM)")
    val dateInput = StdIn.readLine()

    try {
      val reminderDate = parseTimestamp(s"$dateInput:00")
      val remainder = Remainder(name, reminderDate)

      val request = basicRequest
        .delete(serverUrl)
        .body(remainder.asJson)
        .response(asString)

      val response = request.send(backend)

      response.body match {
        case Right(message) =>
          println(s"Remainder deleted successfully")
        case Left(error) =>
          println(s"Failed to delete remainder: $error")
      }
    } catch {
      case e: Exception =>
        println(s"Invalid date format: ${e.getMessage}")
    }
  }

  private def getAllRemainders(): Unit = {
    val request = basicRequest
      .get(uri"http://localhost:8080/api/v1/remainders")
      .response(asJson[List[Remainder]])

    val response = request.send(backend)

    response.body match {
      case Right(remainders) =>
        println("All Remainders:")
        remainders.foreach(println)
      case Left(error) =>
        println(s"Failed to fetch remainders: $error")
    }
  }


  def main(args: Array[String]): Unit = {
    var continue = true
    println("Welcome to Remainder CLI!")

    while (continue) {
      println("Choose an option:")
      println("1. Create a remainder")
      println("2. Delete a remainder")
      println("3. See all remainders")
      println("4. Exit")
      print("\nEnter your choice (1-4): ")
      StdIn.readLine() match {
        case "1" => createRemainder()
        case "2" => deleteRemainder()
        case "3" => getAllRemainders()
        case "4" =>
          println("Exiting...")
          continue = false
        case _ =>
          println("Invalid option. Please try again.")
      }
    }
  }
}
