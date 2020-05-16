package hu.bme.sch.sssl.doktor.repository

import java.util.UUID

import hu.bme.sch.sssl.doktor.`enum`.MessageStatus.MessageStatus
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, Rep}

import scala.concurrent.{ExecutionContext, Future}

class MessageRepository(
    implicit
    db: Database,
    ec: ExecutionContext,
) {
  import MessageRepository._

  private[repository] val messages = TableQuery[MessageTable]

  def upsert(dbo: MessageDbo): Future[Int] = {
    val upsertQuery = for {
      existing <- messages.filter(_.messageId === dbo.messageId).result.headOption
      row =
        existing.map(_.copy(status = dbo.status, reviewedByUid = dbo.reviewedByUid, reviewedBy = dbo.reviewedBy, reviewedAt = dbo.reviewedAt)).getOrElse(dbo)
      result <- messages.insertOrUpdate(row)
    } yield result

    db.run(upsertQuery)
  }

  def findById(messageId: UUID): Future[Option[MessageDbo]] =
    db.run(messages.filter(_.messageId === messageId).result.headOption)

  def findByTicketId(ticketId: UUID): Future[Seq[MessageDbo]] =
    db.run(messages.filter(_.ticketId === ticketId).result)

  def listAllWithStatusFilter(status: MessageStatus): Future[Seq[MessageDbo]] =
    db.run(messages.filter(_.status === status).result)

  private[repository] class MessageTable(tag: Tag) extends Table[MessageDbo](tag, "messages") {
    def id: Rep[Long]                      = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def messageId: Rep[UUID]               = column[UUID]("message_id", O.Unique)
    def ticketId: Rep[UUID]                = column[UUID]("ticket_id")
    def uid: Rep[String]                   = column[String]("uid")
    def createdBy: Rep[String]             = column[String]("created_by")
    def createdAt: Rep[Long]               = column[Long]("created_at")
    def status: Rep[MessageStatus]         = column[MessageStatus]("status")
    def text: Rep[String]                  = column[String]("text")
    def reviewedByUid: Rep[Option[String]] = column[Option[String]]("reviewed_by_uid")
    def reviewedBy: Rep[Option[String]]    = column[Option[String]]("reviewed_by")
    def reviewedAt: Rep[Option[Long]]      = column[Option[Long]]("reviewed_at")

    override def * : ProvenShape[MessageDbo] =
      (
        messageId,
        ticketId,
        uid,
        createdBy,
        createdAt,
        status,
        text,
        reviewedByUid,
        reviewedBy,
        reviewedAt,
        id,
      ) <> (MessageDbo.tupled, MessageDbo.unapply)
  }
}

object MessageRepository {
  case class MessageDbo(
      messageId: UUID,
      ticketId: UUID,
      uid: String,
      createdBy: String,
      createdAt: Long,
      status: MessageStatus,
      text: String,
      reviewedByUid: Option[String],
      reviewedBy: Option[String],
      reviewedAt: Option[Long],
      id: Long = 0L,
  )
}
