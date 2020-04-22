package hu.bme.sch.sssl.doktor.util

import cats.data.EitherT
import spray.json._

import scala.concurrent.Future

object ErrorUtil {
  import DefaultJsonProtocol._

  sealed abstract class AppError {
    def message: String
  }

  type AppErrorOr[T] = EitherT[Future, AppError, T]

  case class UnsuccessfulAction(message: String) extends AppError

  implicit val unsuccessfulActionFormatter: RootJsonFormat[UnsuccessfulAction] = jsonFormat1(UnsuccessfulAction)

  implicit val writer: RootJsonWriter[AppError] = {
    case obj: UnsuccessfulAction => obj.toJson
  }
}
