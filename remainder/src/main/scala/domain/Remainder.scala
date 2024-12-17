package domain

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema
import sttp.tapir.generic.auto.*

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

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

final case class Remainder(name: String, reminderDate: Timestamp)

given timestampSchema: Schema[Timestamp] = Schema.string
given remainderSchema: Schema[Remainder] = Schema.derived[Remainder]
