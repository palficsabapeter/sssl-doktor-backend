package hu.bme.sch.sssl.doktor.`enum`

import spray.json.{JsValue, JsonFormat}
import sttp.tapir.{Schema, SchemaType, Validator}

import spray.json._

import scala.util.Try

trait SerializableEnumeration { e: CustomEnumeration =>
  import io.circe._

  implicit val enumDecoder: Decoder[e.EnumType] = Decoder.decodeEnumeration(e)
  implicit val enumEncoder: Encoder[e.EnumType] = Encoder.encodeEnumeration(e)

  implicit val schemaForEnum: Schema[e.EnumType]       = Schema(SchemaType.SString)
  implicit def validatorForEnum: Validator[e.EnumType] = Validator.`enum`(e.values.toList, v => Option(v))

  implicit object CustomEnumerationJsonFormat extends JsonFormat[e.EnumType] {
    def write(obj: e.EnumType): JsValue = JsString(obj.toString)

    def read(json: JsValue): e.EnumType =
      json match {
        case JsString(str) =>
          Try(withName(str)).getOrElse(throw DeserializationException(s"Enumeration $str doesn't exist $e [${e.values.mkString(",")}]"))
        case _ =>
          throw DeserializationException("Type mismatch, this parameter must be a string!")
      }
  }
}
