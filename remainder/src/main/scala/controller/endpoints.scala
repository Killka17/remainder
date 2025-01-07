package remainder.controller

import cats.effect.IO
import io.circe.generic.auto.*
import io.circe.{Decoder, Encoder}
import remainder.domain.*
import remainder.domain.Errors.AppError
import remainder.service.RemainderStorage
import sttp.tapir.*
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class endpoints(storage: RemainderStorage) {

  given timestampCodec: Codec[String, Timestamp, TextPlain] = Codec.string.mapDecode { str =>
    try {
      val parsed = LocalDateTime.parse(str, timestampFormatter)
      DecodeResult.Value(Timestamp.valueOf(parsed))
    } catch {
      case e: Exception => DecodeResult.Error(str, e)
    }
  }(_.toLocalDateTime.format(timestampFormatter))

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

  val allRemaindersWithLogic: ServerEndpoint[Any, IO] = allRemainders.serverLogic { _ =>
    storage.list.map {
      case Right(remainders) => Right(remainders)
      case Left(error)       => Left(Errors.InternalError(error.msg))
    }
  }

  val findRemainderByDate: PublicEndpoint[String, AppError, List[String], Any] =
    endpoint.get
      .in("api" / "v1" / "remainder")
      .in(query[String]("date"))
      .errorOut(jsonBody[AppError])
      .out(jsonBody[List[String]])

  val findRemainderByDateWithLogic: ServerEndpoint[Any, IO] = findRemainderByDate.serverLogic { dateStr =>
    try {
      val date = Timestamp.valueOf(LocalDateTime.parse(dateStr, timestampFormatter))
      storage.findByDate(date).map {
        case Right(remainders) => Right(remainders)
        case Left(error)       => Left(Errors.InternalError(error.msg))
      }
    } catch {
      case e: Exception => IO.pure(Left(Errors.InternalError(s"Неверный формат даты: $dateStr")))
    }
  }

  val insertRemainder: PublicEndpoint[Remainder, AppError, Remainder, Any] =
    endpoint.post
      .in("api" / "v1" / "remainder")
      .in(jsonBody[Remainder])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Remainder])

  val insertRemainderWithLogic: ServerEndpoint[Any, IO] = insertRemainder.serverLogic { remainder =>
    storage.insert(remainder).map {
      case Right(inserted) => Right(inserted)
      case Left(error)     => Left(Errors.InternalError(error.msg))
    }
  }

  val removeRemainder: PublicEndpoint[Remainder, AppError, Unit, Any] =
    endpoint.delete
      .in("api" / "v1" / "remainder")
      .in(jsonBody[Remainder])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Unit])

  val removeRemainderWithLogic: ServerEndpoint[Any, IO] = removeRemainder.serverLogic { remainder =>
    storage.remove(remainder).map(_ => Right(()))
  }
}
