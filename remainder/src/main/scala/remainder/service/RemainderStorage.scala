package remainder.service

import cats.effect.IO
import cats.implicits.{catsSyntaxEither, catsSyntaxEitherId}
import doobie.Transactor
import doobie.implicits.toConnectionIOOps
import remainder.dbclient.SqlClient
import remainder.domain.{Errors, Remainder}
import tofu.logging.Logging
import tofu.syntax.logging.*
import remainder.domain.Errors.InternalError

import java.sql.Timestamp

trait RemainderStorage {
  def findByDate(date: Timestamp): IO[Either[Errors.AppError, List[String]]]
  def insert(remainder: Remainder): IO[Either[Errors.AppError, Remainder]]
  def remove(remainder: Remainder): IO[Either[Errors.AppError, Unit]]
  def list: IO[Either[Errors.InternalError, List[Remainder]]]
}

object RemainderStorage {
  private final class Impl(sqlClient: SqlClient, transactor: Transactor[IO]) extends RemainderStorage {
    override def findByDate(date: Timestamp): IO[Either[Errors.AppError, List[String]]] = sqlClient
      .findByDate(date)
      .transact(transactor)
      .attempt
      .map(_.leftMap(_ => Errors.UnableToAccessDatabase))

    override def insert(remainder: Remainder): IO[Either[Errors.AppError, Remainder]] =
      sqlClient.insert(remainder).transact(transactor).attempt.map {
        case Left(err)               => InternalError(err.getMessage).asLeft
        case Right(Left(err))        => err.asLeft
        case Right(Right(remainder)) => remainder.asRight
      }

    override def remove(remainder: Remainder): IO[Either[Errors.AppError, Unit]] =
      sqlClient.remove(remainder).transact(transactor).attempt.map {
        case Left(err)        => InternalError(err.getMessage).asLeft
        case Right(Left(err)) => err.asLeft
        case Right(_)         => ().asRight
      }

    override def list: IO[Either[Errors.InternalError, List[Remainder]]] =
      sqlClient.allRemainders
        .transact(transactor)
        .attempt
        .map(_.leftMap { (e: Throwable) =>
          InternalError(e.getMessage)
        })

  }

  private final class LoggingImpl(storage: RemainderStorage)(using logging: Logging[IO]) extends RemainderStorage {

    override def list: IO[Either[Errors.InternalError, List[Remainder]]] = {
      info"Starting method list" *> storage.list.flatTap {
        case Left(err)     => error"Error occurred in list: ${err.toString}" *> IO.pure(Left(err))
        case Right(result) => info"list result: ${result.toString}" *> IO.pure(Right(result))
      }
    }

    override def findByDate(date: Timestamp): IO[Either[Errors.AppError, List[String]]] = {
      info"Starting method findByDate with date: $date" *> storage.findByDate(date).flatTap {
        case Left(err)     => error"Error occurred in findByDate: ${err.toString}" *> IO.pure(Left(err))
        case Right(result) => info"findByDate result: $result" *> IO.pure(Right(result))
      }
    }

    override def insert(remainder: Remainder): IO[Either[Errors.AppError, Remainder]] = {
      info"Starting method insert with remainder: ${remainder.name} ${remainder.reminderDate}" *> storage
        .insert(remainder)
        .flatTap {
          case Left(err) => error"Error occurred in insert: ${err.toString}" *> IO.pure(Left(err))
          case Right(result) =>
            info"Insert successful, result: ${result.name} ${result.reminderDate}" *> IO.pure(Right(result))
        }
    }

    override def remove(remainder: Remainder): IO[Either[Errors.AppError, Unit]] = {
      info"Starting method remove with remainder: ${remainder.name} ${remainder.reminderDate}" *> storage
        .remove(remainder)
        .flatTap {
          case Left(err) => error"Error occurred in remove: ${err.toString}" *> IO.pure(Left(err))
          case Right(_)  => info"Remove successful" *> IO.pure(Right(()))
        }
    }

  }
  def make(sqlClient: SqlClient, transactor: Transactor[IO]): RemainderStorage = {
    implicit val logs: Logging.Make[IO] = Logging.Make.plain[IO]

    implicit val logging: Logging[IO] = logs.forService[RemainderStorage]

    val impl = new Impl(sqlClient, transactor)
    new LoggingImpl(impl)
  }
}
