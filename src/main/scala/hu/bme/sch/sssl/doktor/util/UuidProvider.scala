package hu.bme.sch.sssl.doktor.util

import java.util.UUID

trait UuidProvider {
  def generateUuid: UUID
}

object UuidProvider {
  def apply() =
    new UuidProvider {
      override def generateUuid: UUID = UUID.randomUUID()
    }
}
