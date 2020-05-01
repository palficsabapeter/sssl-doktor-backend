package hu.bme.sch.sssl.doktor.`enum`

object TicketType extends CustomEnumeration with SerializableEnumeration with PersistableEnumeration {
  type TicketType = Value

  val FeedbackRequest: TicketType = Value("feedback-request")
  val Criticism: TicketType       = Value("criticism")
  val AdviceRequest: TicketType   = Value("advice-request")
  val Misc: TicketType            = Value("misc")
}
