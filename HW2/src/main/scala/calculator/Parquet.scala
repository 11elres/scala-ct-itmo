package calculator

import scala.math.pow

object Parquet {

  def calculate(building: Building): Int = building match {
    case Building.Premium(length, width, height, floorNumber) =>
      pow(if floorNumber < 5 then 3 else 2, floorNumber).intValue() * (length + width + height)
    case Building.Economy(length, width, height, floorNumber) =>
      length * width * height + floorNumber * 10000
  }

}
