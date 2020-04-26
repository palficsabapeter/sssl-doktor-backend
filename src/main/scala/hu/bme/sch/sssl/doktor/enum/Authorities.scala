package hu.bme.sch.sssl.doktor.`enum`

object Authorities extends CustomEnumeration with SerializableEnumeration with PersistableEnumeration {
  type Authorities = Value

  val Admin: Authorities = Value("ADMIN")
  val Clerk: Authorities = Value("CLERK")
  val User: Authorities  = Value("USER")
}
