package hu.bme.sch.sssl.doktor.service

import java.util.UUID

import cats.data.EitherT
import hu.bme.sch.sssl.doktor.`enum`.MessageStatus
import hu.bme.sch.sssl.doktor.repository.MessageRepository
import hu.bme.sch.sssl.doktor.util.ErrorUtil.{AppErrorOr, MessageNotFound}
import hu.bme.sch.sssl.doktor.util.TimeProvider

import scala.concurrent.ExecutionContext

class ApproveMessageService(
    implicit
    repo: MessageRepository,
    tp: TimeProvider,
    ec: ExecutionContext,
) {
  def approveMessage(ticketId: UUID, messageId: UUID, approved: Boolean, reviewedBy: String, reviewedByUid: String): AppErrorOr[Unit] = {
    val res = repo
      .findByTicketId(ticketId)
      .map { messages =>
        messages
          .find(_.messageId == messageId)
          .map { message =>
            repo.upsert(
              message.copy(
                status = if (approved) MessageStatus.Shown else MessageStatus.Discarded,
                reviewedAt = Some(tp.epochMillis),
                reviewedBy = Some(reviewedBy),
                reviewedByUid = Some(reviewedByUid),
              ),
            )
          }
          .map(_ => Right({}))
          .getOrElse(Left(MessageNotFound(s"Message with id '$messageId' and related ticket id '$ticketId' was not found!")))
      }

    EitherT(res)
  }
}
