package hu.bme.sch.sssl.doktor.repository

import java.util.UUID

import hu.bme.sch.sssl.doktor.`enum`.TicketType.TicketType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, Rep}

import scala.concurrent.{ExecutionContext, Future}

class TicketRepository(
    implicit
    db: Database,
    ec: ExecutionContext,
) {
  import TicketRepository._

  private[repository] val tickets = TableQuery[TicketTable]

  def upsert(dbo: TicketDbo): Future[Int] = {
    val upsertQuery = for {
      existing <- tickets.filter(_.ticketId === dbo.ticketId).result.headOption
      row       = existing.map(_.copy(description = dbo.description, assignedTo = dbo.assignedTo)).getOrElse(dbo)
      result   <- tickets.insertOrUpdate(row)
    } yield result

    db.run(upsertQuery)
  }

  def findById(ticketId: UUID): Future[Option[TicketDbo]] =
    db.run(tickets.filter(_.ticketId === ticketId).result.headOption)

  def findByUserId(uid: String): Future[Seq[TicketDbo]] =
    db.run(tickets.filter(_.uid === uid).result)

  def findByAssignedUserId(uid: String): Future[Seq[TicketDbo]] =
    db.run(tickets.filter(_.assignedTo.map(_ === uid)).result)

  private[repository] class TicketTable(tag: Tag) extends Table[TicketDbo](tag, "tickets") {
    def id: Rep[Long]                   = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def ticketId: Rep[UUID]             = column[UUID]("ticket_id", O.Unique)
    def uid: Rep[String]                = column[String]("uid")
    def createdBy: Rep[String]          = column[String]("created_by")
    def createdAt: Rep[Long]            = column[Long]("created_at")
    def ticketType: Rep[TicketType]     = column[TicketType]("ticket_type")
    def isAnonym: Rep[Boolean]          = column[Boolean]("is_anonym")
    def description: Rep[String]        = column[String]("description")
    def assignedTo: Rep[Option[String]] = column[Option[String]]("assigned_to")

    override def * : ProvenShape[TicketDbo] =
      (
        ticketId,
        uid,
        createdBy,
        createdAt,
        ticketType,
        isAnonym,
        description,
        assignedTo,
        id,
      ) <> (TicketDbo.tupled, TicketDbo.unapply)
  }
}

object TicketRepository {
  case class TicketDbo(
      ticketId: UUID,
      uid: String,
      createdBy: String,
      createdAt: Long,
      ticketType: TicketType,
      isAnonym: Boolean,
      description: String,
      assignedTo: Option[String],
      id: Long = 0L,
  )
}
