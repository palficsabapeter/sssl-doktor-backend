package hu.bme.sch.sssl.doktor.util

import cats.data.EitherT

import scala.concurrent.Future

object ErrorUtil {
  sealed trait AppError {
    def message: String
  }

  type AppErrorOr[T] = EitherT[Future, AppError, T]

  case class AuthError(message: String)            extends AppError
  case class DbUnavailable(message: String)        extends AppError
  case class DbActionUnsuccessful(message: String) extends AppError
  case class UnsuccessfulAction(message: String)   extends AppError
  case class TicketNotFound(message: String)       extends AppError
  case class MessageNotFound(message: String)      extends AppError
}
