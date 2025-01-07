package remainder.controller

import io.circe.generic.auto.*
import io.circe.{Decoder, Encoder}
import remainder.domain.*
import remainder.domain.Errors.AppError
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object endpoints {

  private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  
  given timestampEncoder: Encoder[Timestamp] = Encoder.encodeString.contramap { ts =>
    ts.toLocalDateTime.format(timestampFormatter)
  }

  given timestampDecoder: Decoder[Timestamp] = Decoder.decodeString.emap { str =>
    try {
      val parsed = LocalDateTime.parse(str, timestampFormatter)
      Right(Timestamp.valueOf(parsed))
    } catch {
      case e: Exception => Left(s"Invalid Timestamp format: $str")
    }
  }

  given timestampSchema: Schema[Timestamp] = Schema.string

  given remainderSchema: Schema[Remainder] = Schema.derived[Remainder]
  
  val allRemainders: PublicEndpoint[Unit, AppError, List[Remainder], Any] = {
    endpoint.get
      .in("api" / "v1" / "remainders")
      .errorOut(jsonBody[AppError])
      .out(jsonBody[List[Remainder]])
  }

  val findRemainderByDate: PublicEndpoint[String, AppError, List[String], Any] =
    endpoint.get
      .in("api" / "v1" / "remainder")
      .in(query[String]("date"))
      .errorOut(jsonBody[AppError])
      .out(jsonBody[List[String]])

  val insertRemainder: PublicEndpoint[Remainder, AppError, Remainder, Any] =
    endpoint.post
      .in("api" / "v1" / "remainder")
      .in(jsonBody[Remainder])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Remainder])

  val removeRemainder: PublicEndpoint[Remainder, AppError, Unit, Any] =
    endpoint.delete
      .in("api" / "v1" / "remainder")
      .in(jsonBody[Remainder])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Unit])
}
