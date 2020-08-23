package hu.bme.sch.sssl.doktor.`enum`

object MessageStatus extends CustomEnumeration with SerializableEnumeration with PersistableEnumeration {
  type MessageStatus = Value

  val Shown: MessageStatus      = Value("shown")
  val Unreviewed: MessageStatus = Value("unreviewed")
  val Discarded: MessageStatus  = Value("discarded")
}
