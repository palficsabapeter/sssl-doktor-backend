package hu.bme.sch.sssl.doktor.repository

import hu.bme.sch.sssl.doktor.`enum`.Authorities
import hu.bme.sch.sssl.doktor.`enum`.Authorities.Authorities
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, Rep, TableQuery, Tag}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AuthRepository(
    implicit
    db: Database,
    ec: ExecutionContext,
) {
  import AuthRepository._

  private[repository] val authorities = TableQuery[UserAuthTable]

  def upsert(dbo: UserAuthDbo): Future[Int] = {
    val upsertQuery = for {
      existing <- authorities.filter(_.uid === dbo.uid).result.headOption
      row       = existing.map(_.copy(authorities = dbo.authorities)).getOrElse(dbo)
      result   <- authorities.insertOrUpdate(row)
    } yield result

    db.run(upsertQuery)
  }

  def findById(uid: String): Future[Option[UserAuthDbo]] =
    db.run(authorities.filter(_.uid === uid).result.headOption)

  implicit private def seqMapper: JdbcType[Seq[Authorities]] with BaseTypedType[Seq[Authorities]] = {
    val separator: String = ";"

    MappedColumnType.base[Seq[Authorities], String](
      seq => seq.mkString(separator),
      str =>
        if (str.isEmpty)
          Nil
        else
          Try(str.split(separator).map(Authorities.withName).toSeq)
            .getOrElse(throw new IllegalArgumentException(s"Couldn't map $str to Seq[Authorities] format from Database")),
    )
  }

  private[repository] class UserAuthTable(tag: Tag) extends Table[UserAuthDbo](tag, "user_auth") {
    def id: Rep[Long]                      = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def uid: Rep[String]                   = column[String]("uid", O.Unique)
    def authorities: Rep[Seq[Authorities]] = column[Seq[Authorities]]("authorities")

    override def * : ProvenShape[UserAuthDbo] =
      (
        uid,
        authorities,
        id,
      ) <> (UserAuthDbo.tupled, UserAuthDbo.unapply)
  }
}

object AuthRepository {
  case class UserAuthDbo(
      uid: String,
      authorities: Seq[Authorities],
      id: Long = 0L,
  )
}
