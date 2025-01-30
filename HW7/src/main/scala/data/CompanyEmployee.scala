package data

import cats.implicits.toBifunctorOps
import unmarshal.decoder.Decoder
import unmarshal.encoder.Encoder
import unmarshal.error.DecoderError
import unmarshal.error.DecoderError.wrongJson
import unmarshal.model.Json.{JsonArray, JsonObject}

case class CompanyEmployee(
  employees: List[Employee]
)

object CompanyEmployee {

  implicit def companyEmployeeEncoder: Encoder[CompanyEmployee] = Encoder.autoDerive

  implicit def companyEmployeeDecoder: Decoder[CompanyEmployee] = {
    case JsonObject(map) =>
      map.get("employees") match {
        case Some(jsonArray: JsonArray) =>
          jsonArray.value.zipWithIndex
            .foldLeft[Either[DecoderError, List[Employee]]](Right(List.empty[Employee])) {
              case (acc, (employee, index)) =>
                acc.flatMap { employees =>
                  employee match {
                    case jsonObject: JsonObject =>
                      Employee.employeeDecoder
                        .fromJson(jsonObject)
                        .leftMap(error =>
                          wrongJson(
                            "one of employees has wrong field",
                            s"employees.$index.${error.field}"
                          )
                        )
                        .map(employee => employees :+ employee)
                    case _ =>
                      Left(
                        wrongJson(
                          "one of employees represents by not JsonObject",
                          s"employees.$index"
                        )
                      )
                  }
                }
            }
            .map(CompanyEmployee(_))
        case _ =>
          Left(wrongJson("", "employees"))
      }
    case other => Left(DecoderError(s"JsonObject expected, but found: $other", ""))
  }

}
