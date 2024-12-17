package domain

import io.circe.generic.semiauto.*

object Errors {

  sealed abstract class AppError(msg: String)

  case class UnableToAccessDatabase() extends AppError("Unable to access database")
  case class RemainderNotFound() extends AppError("Remainder not found")
  case class RemainderAlreadyExist() extends AppError("Remainder already exist")
  case class InternalError(msg: String = "Internal error") extends AppError(msg)
  case class InvalidDateFormat(msg: String = "Invalid date format") extends AppError(msg)

}
