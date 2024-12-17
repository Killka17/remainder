import cats.effect.unsafe.implicits.global
import domain.*
import domain.Errors.AppError
import io.circe.generic.auto.*
import io.circe.{Decoder, Encoder}
import sttp.tapir.*
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

import java.sql.Timestamp
import java.time.LocalDateTime
import domain.timestampDecoder
import domain.timestampEncoder
import domain.timestampSchema

implicit val timestampCodec: Codec[String, Timestamp, TextPlain] = Codec.string.mapDecode { str =>
  try {
    val parsed = LocalDateTime.parse(str, timestampFormatter)
    DecodeResult.Value(Timestamp.valueOf(parsed))
  } catch {
    case e: Exception => DecodeResult.Error(str, e)
  }
}(_.toLocalDateTime.format(timestampFormatter))

object endpoints {

  val allRemainders: Endpoint[Unit, Unit, AppError, List[Remainder], Any] = {
    endpoint.get
      .in("api" / "v1" / "remainders")
      .errorOut(jsonBody[AppError])
      .out(jsonBody[List[Remainder]])
  }

  val allRemaindersWithLogic = allRemainders.serverLogic { _ =>
    storage.list.unsafeToFuture()
  }

  val findRemainderByDate =
    endpoint.get
      .in("api" / "v1" / "remainder")
      .in(query[Timestamp]("date"))
      .errorOut(jsonBody[AppError])
      .out(jsonBody[List[String]])

  val findRemainderByDateWithLogic = findRemainderByDate.serverLogic { date =>
    storage.findByDate(date).unsafeToFuture()
  }

  val insertRemainder =
    endpoint.post
      .in("api" / "v1" / "remainder")
      .in(jsonBody[Remainder])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Remainder])

  val insertRemainderWithLogic = insertRemainder.serverLogic { remainder =>
    storage.insert(remainder).unsafeToFuture()
  }

  val removeRemainder =
    endpoint.delete
      .in("api" / "v1" / "remainder")
      .in(jsonBody[Remainder])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Unit])

  val removeRemainderWithLogic = removeRemainder.serverLogic { remainder =>
    storage.remove(remainder).unsafeToFuture()
  }
}
