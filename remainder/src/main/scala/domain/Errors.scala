package remainder.domain

object Errors {

  sealed trait AppError {
    def msg: String
  }

  case object UnableToAccessDatabase extends AppError {
    override def msg: String = "Unable to access database"
  }

  case object RemainderNotFound extends AppError {
    override def msg: String = "Remainder not found"
  }

  case object RemainderAlreadyExist extends AppError {
    override def msg: String = "Remainder already exist"
  }

  case class InternalError(override val msg: String = "Internal error") extends AppError

  case class InvalidDateFormat(override val msg: String = "Invalid date format") extends AppError

}
