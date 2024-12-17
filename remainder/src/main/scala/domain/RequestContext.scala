package domain

import cats.data.ReaderT
import cats.effect.IO
import sttp.tapir.Codec
import sttp.tapir.CodecFormat.TextPlain

final case class RequestContext(requestDate: String)
object RequestContext {
  implicit val codec: Codec[String, RequestContext, TextPlain] =
    Codec.string.map(RequestContext(_))(_.requestDate)

  type ContextualIO[T] = ReaderT[IO, RequestContext, T]
}
