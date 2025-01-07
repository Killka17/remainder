package remainder.controller

import cats.effect.IO
import remainder.domain.Errors
import remainder.service.RemainderStorage
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{Codec, DecodeResult}

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EndpointsWithLogic(storage: RemainderStorage) {

  private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  given timestampCodec: Codec[String, Timestamp, TextPlain] = Codec.string.mapDecode { str =>
    try {
      val parsed = LocalDateTime.parse(str, timestampFormatter)
      DecodeResult.Value(Timestamp.valueOf(parsed))
    } catch {
      case e: Exception => DecodeResult.Error(str, e)
    }
  }(_.toLocalDateTime.format(timestampFormatter))

  val allRemaindersWithLogic: ServerEndpoint[Any, IO] = endpoints.allRemainders.serverLogic { _ =>
    storage.list.map {
      case Right(remainders) => Right(remainders)
      case Left(error)       => Left(Errors.InternalError(error.msg))
    }
  }

  val findRemainderByDateWithLogic: ServerEndpoint[Any, IO] = endpoints.findRemainderByDate.serverLogic { dateStr =>
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

  val insertRemainderWithLogic: ServerEndpoint[Any, IO] = endpoints.insertRemainder.serverLogic { remainder =>
    storage.insert(remainder).map {
      case Right(inserted) => Right(inserted)
      case Left(error)     => Left(Errors.InternalError(error.msg))
    }
  }

  val removeRemainderWithLogic: ServerEndpoint[Any, IO] = endpoints.removeRemainder.serverLogic { remainder =>
    storage.remove(remainder).map(_ => Right(()))
  }
  val all: List[ServerEndpoint[Any, IO]] =
    List(allRemaindersWithLogic, findRemainderByDateWithLogic, insertRemainderWithLogic, removeRemainderWithLogic)

}
