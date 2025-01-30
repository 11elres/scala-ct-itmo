package building

import scala.annotation.tailrec

import Errors.{
  ResidentAgeNonPositiveException,
  CommercialFloorWithoutCommercialsException,
  IncorrectBuildingContentException
}

/** Здание должно иметь:
  *   - строковый адрес
  *   - этажи (сходящиеся к первому этажу) Этаж может быть жилым, коммерческим, либо чердаком
  *     (который сам может быть коммерческим). На каждом жилом этаже живет 2 человека и есть
  *     лестница(ссылка) ведущая на следующий этаж У каждого человека есть возраст (>0) и пол На
  *     коммерческом этаже может быть несколько заведений (используйте Array), но не меньше 1.
  *     Здание всегда должно заканчиваться чердаком На чердаке никто не живет, но это может быть и
  *     коммерческое помещение (но только 1).
  */

class Building(val address: String, val firstFloor: Floor)

sealed trait Floor
sealed trait MiddleFloor(val nextFloor: Floor) extends Floor

case class ResidentialFloor(resident1: Resident, resident2: Resident, override val nextFloor: Floor)
    extends MiddleFloor(nextFloor)

case class CommercialFloor(commercials: Array[Commercial], override val nextFloor: Floor)
    extends MiddleFloor(nextFloor)

sealed trait Attic                                 extends Floor
case class CommonAttic()                           extends Attic
case class CommercialAttic(commercial: Commercial) extends Attic

case class Commercial(name: String)
case class Resident(age: Int, sex: Sex)

sealed trait Sex
case object Male   extends Sex
case object Female extends Sex

object Building {

  def apply(address: String, firstFloor: Floor): Either[Exception, Building] =
    @tailrec
    def checkCorrect(floor: Floor): Boolean = floor match
      case CommercialFloor(commercials, nextFloor) =>
        if commercials.length == 0 then false else checkCorrect(nextFloor)
      case ResidentialFloor(Resident(age1, _), Resident(age2, _), nextFloor) =>
        if age1 > 0 && age2 > 0 then checkCorrect(nextFloor) else false
      case _ => true

    if checkCorrect(firstFloor)
    then Right(new Building(address, firstFloor))
    else Left(new IncorrectBuildingContentException)

  /** Проходится по зданию снизу в вверх, применяя функцию [[f]] на каждом жилом этаже с начальным
    * аккумулятором [[accumulator]]
    */
  def fold(building: Building, accumulator: Int)(f: (Int, Floor) => Int): Int =
    @tailrec
    def foldFloor(floor: Floor, acc: Int)(f: (Int, Floor) => Int): Int = floor match
      case middleFloor: MiddleFloor => foldFloor(middleFloor.nextFloor, f(acc, floor))(f)
      case _                        => f(acc, floor)

    foldFloor(building.firstFloor, accumulator)(f)

  /** Подсчитывает количество этаже, на которых живет хотя бы один мужчина старше [[olderThan]].
    * Используйте [[fold]]
    */
  def countOldManFloors(building: Building, olderThan: Int): Int =
    def f(count: Int, floor: Floor): Int = count + (floor match
      case ResidentialFloor(Resident(age, Male), _, _) if age > olderThan => 1
      case ResidentialFloor(_, Resident(age, Male), _) if age > olderThan => 1
      case _                                                              => 0
    )

    fold(building, 0)(f)

  /** Находит наибольший возраст женщины, проживающей в здании. Используйте [[fold]] */
  def womanMaxAge(building: Building): Option[Int] =
    def f(maxAge: Int, floor: Floor): Int = Math.max(
      maxAge,
      floor match
        case ResidentialFloor(Resident(age1, Female), Resident(age2, Female), _) =>
          Math.max(age1, age2)
        case ResidentialFloor(Resident(age, Female), _, _) => age
        case ResidentialFloor(_, Resident(age, Female), _) => age
        case _                                             => 0
    )

    val answer: Int = fold(building, 0)(f)
    if answer == 0 then None else Some(answer)

  /** Находит кол-во коммерческих заведений в здании. Используйте [[fold]] */
  def countCommercial(building: Building): Int =
    def f(count: Int, floor: Floor): Int = count + (floor match
      case CommercialFloor(commercials, _) => commercials.length
      case CommercialAttic(_)              => 1
      case _                               => 0
    )

    fold(building, 0)(f)

  /** Находит среднее кол-во коммерческих заведений зданиях. Реализуйте свою функцию, похожую на
    * [[fold]] для прохода по зданию
    */
  def countCommercialAvg(buildings: Array[Building]): Double =
    @tailrec
    def foldBuildingArray(buildings: Array[Building], index: Int, accumulator: Int)(
      f: (Int, Building) => Int
    ): Int =
      if index == buildings.length
      then accumulator
      else foldBuildingArray(buildings, index + 1, f(accumulator, buildings(index)))(f)

    if buildings.length == 0
    then 0
    else
      foldBuildingArray(buildings, 0, 0)((amountCommercials: Int, building: Building) =>
        amountCommercials + countCommercial(building)
      ).toDouble / buildings.length

  /** Находит среднее кол-во мужчин на четных этажах. Реализуйте свою функцию, похожую на [[fold]]
    * для прохода по зданию
    */
  def evenFloorsMenAvg(building: Building): Double =
    def foldEvenFloors(building: Building, accumulator: (Int, Int))(
      f: ((Int, Int), Floor) => (Int, Int)
    ): (Int, Int) =
      @tailrec
      def foldFloor(floor: Floor, acc: (Int, Int))(
        f: ((Int, Int), Floor) => (Int, Int)
      ): (Int, Int) = floor match
        case middleFloor: MiddleFloor =>
          middleFloor.nextFloor match
            case middleFloor2: MiddleFloor => foldFloor(middleFloor2.nextFloor, f(acc, floor))(f)
            case _                         => f(acc, floor)
        case _ => acc

      building.firstFloor match
        case mf: MiddleFloor => foldFloor(mf.nextFloor, accumulator)(f)
        case _               => accumulator

    foldEvenFloors(building, (0, 0))((statistics: (Int, Int), floor: Floor) =>
      (
        statistics(0) + (floor match
          case ResidentialFloor(Resident(_, Male), Resident(_, Male), _) => 2
          case ResidentialFloor(Resident(_, Male), _, _)                 => 1
          case ResidentialFloor(_, Resident(_, Male), _)                 => 1
          case _                                                         => 0
        ),
        statistics(1) + 1
      )
    ) match
      case (_, 0)                    => 0.0
      case (amountMen, amountFloors) => amountMen.toDouble / amountFloors

}

object CommercialFloor {

  def apply(commercials: Array[Commercial], nextFloor: Floor): Either[Exception, CommercialFloor] =
    if commercials.length == 0
    then Left(new CommercialFloorWithoutCommercialsException)
    else Right(new CommercialFloor(commercials, nextFloor))

}

object Resident {

  def apply(age: Int, sex: Sex): Either[Exception, Resident] =
    if age <= 0
    then Left(new ResidentAgeNonPositiveException)
    else Right(new Resident(age, sex))

}
