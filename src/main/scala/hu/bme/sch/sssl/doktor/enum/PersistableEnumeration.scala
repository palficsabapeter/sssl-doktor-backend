package hu.bme.sch.sssl.doktor.`enum`

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

import scala.util.Try

trait PersistableEnumeration { enum: CustomEnumeration =>
  implicit val enumMapper: JdbcType[enum.EnumType] with BaseTypedType[enum.EnumType] =
    MappedColumnType.base[enum.EnumType, String](
      e => e.toString,
      s => Try(withName(s)).getOrElse(throw new IllegalArgumentException(s"Enum $s doesn't exist in $enum [${enum.values.mkString(",")}]")),
    )
}
