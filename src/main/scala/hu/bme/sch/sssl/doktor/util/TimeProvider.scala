package hu.bme.sch.sssl.doktor.util

trait TimeProvider {
  def epochMillis: Long
  def epochSecs: Long
}

object TimeProvider {
  def apply() =
    new TimeProvider {
      override def epochMillis: Long = System.currentTimeMillis
      override def epochSecs: Long   = epochMillis / 1000
    }
}
