package remainder.dbСlient

import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId}
import doobie.ConnectionIO
import doobie.implicits.javasql.TimestampMeta
import doobie.implicits.toSqlInterpolator
import doobie.util.query.Query0
import doobie.util.update.Update0
import remainder.domain.{Errors, Remainder}

import java.sql.Timestamp
import java.time.format.DateTimeFormatter

trait SqlClient {
  def findByDate(date: Timestamp): ConnectionIO[List[String]]
  def insert(remainder: Remainder): ConnectionIO[Either[Errors.AppError, Remainder]]
  def remove(remainder: Remainder): ConnectionIO[Either[Errors.AppError, Unit]]
  def allRemainders: ConnectionIO[List[Remainder]]
}

object SqlClient {
  object sql {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    def allRemaindersSql: Query0[Remainder] = sql"""
      SELECT remainder_text, remainder_time
      FROM remainder.remainders
    """.query[Remainder]

    def findByDateSql(date: Timestamp): ConnectionIO[List[String]] = {
      val formattedDate = date.toLocalDateTime.format(formatter)
      sql"""
        SELECT remainder_text
        FROM remainder.remainders
        WHERE remainder_time = $formattedDate::timestamp
      """.query[String].to[List]
    }

    def insertSql(remainder: Remainder): ConnectionIO[Int] = {
      val formattedDate = remainder.reminderDate.toLocalDateTime.format(formatter)
      sql"insert into remainder.remainders(remainder_time, remainder_text) values ($formattedDate::timestamp, ${remainder.name})".update.run
    }

    def removeSql(remainder: Remainder): Update0 = {
      val formattedDate = remainder.reminderDate.toLocalDateTime.format(formatter)
      sql"delete from remainder.remainders where remainder_time = $formattedDate::timestamp and remainder_text = ${remainder.name}".update
    }

    def findByDateAndNameSql(name: String, date: Timestamp): Query0[Remainder] = {
      val formattedDate = date.toLocalDateTime.format(formatter)
      sql"""
        SELECT *
        FROM remainder.remainders
        WHERE remainder_time = $formattedDate::timestamp and remainder_text = $name
      """.query[Remainder]
    }
  }
}

final class Impl extends SqlClient {
  import SqlClient.sql.*

  override def allRemainders: ConnectionIO[List[Remainder]] = allRemaindersSql.to[List]

  override def findByDate(date: Timestamp): ConnectionIO[List[String]] = findByDateSql(date)
  override def insert(remainder: Remainder): ConnectionIO[Either[Errors.AppError, Remainder]] = {
    findByDateAndNameSql(remainder.name, remainder.reminderDate).option
      .flatMap {
        case Some(_) => Errors.RemainderAlreadyExist.asLeft.pure[ConnectionIO]
        case None    => insertSql(remainder).map(_ => remainder.asRight)
      }
  }
  override def remove(remainder: Remainder): ConnectionIO[Either[Errors.AppError, Unit]] =
    removeSql(remainder).run.map {
      case 0 => Errors.RemainderNotFound.asLeft
      case _ => ().asRight
    }
}
