package data

import unmarshal.decoder.Decoder
import unmarshal.encoder.Encoder
import unmarshal.error.DecoderError
import unmarshal.error.DecoderError.wrongJson
import unmarshal.model.Json
import unmarshal.model.Json.{JsonNull, JsonNum, JsonObject, JsonString}

case class Employee(
  name: String,
  age: Long,
  id: Long,
  bossId: Option[Long]
)

object Employee {

  implicit def employeeEncoder: Encoder[Employee] = Encoder.autoDerive

  implicit def employeeDecoder: Decoder[Employee] = {
    case JsonObject(map) =>
      def getFromWrong(
        got: Option[Json],
        field: String,
        expected: String
      ): Either[DecoderError, Nothing] = got match {
        case Some(x) =>
          Left(wrongJson(s"expected $expected, but found ${x.getClass.getSimpleName}", field))
        case _ => Left(wrongJson("not found field", field))
      }

      def getString(field: String): Either[DecoderError, String] = map.get(field) match {
        case Some(JsonString(value)) => Right(value)
        case wrong                   => getFromWrong(wrong, field, "JsonString")
      }

      def getLong(field: String): Either[DecoderError, Long] = map.get(field) match {
        case Some(JsonNum(value)) => Right(value)
        case wrong                => getFromWrong(wrong, field, "JsonNum")
      }

      def getOptionLong(field: String): Either[DecoderError, Option[Long]] =
        map.get(field) match {
          case Some(JsonNull)       => Right(None)
          case Some(JsonNum(value)) => Right(Some(value))
          case wrong                => getFromWrong(wrong, field, "JsonNum or JsonNull")
        }

      val expectedFields = Set("name", "age", "id", "bossId")
      val excessFields   = map.keySet -- expectedFields

      if (excessFields.nonEmpty)
        Left(wrongJson("", excessFields.head))
      else
        for {
          name   <- getString("name")
          age    <- getLong("age")
          id     <- getLong("id")
          bossId <- getOptionLong("bossId")
        } yield Employee(name, age, id, bossId)
    case other => Left(DecoderError(s"JsonObject expected, but found: $other", ""))
  }

}
