package building

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import Errors.{
  CommercialFloorWithoutCommercialsException,
  ResidentAgeNonPositiveException,
  IncorrectBuildingContentException
}

class BuildingSpec extends AnyFlatSpec with Matchers {

  // specs on constructor validations

  "Building" should "be invalid if content don't observed model" in {
    (Building(
      "54 avenue, 74-98",
      new CommercialFloor(
        Array(),
        ResidentialFloor(new Resident(-1, Male), new Resident(0, Female), CommonAttic())
      )
    ) match
      case Left(ex: IncorrectBuildingContentException) => true
      case _                                           => false
    ) shouldEqual true
  }

  "CommercialFloor" should "be invalid if commercials is zero-length array" in {
    (CommercialFloor(
      Array(),
      CommonAttic()
    ) match
      case Left(_: CommercialFloorWithoutCommercialsException) => true
      case _                                                   => false
    ) shouldEqual true
  }

  "Resident" should "be invalid if age is non positive" in {
    (Resident(0, Male) match
      case Left(_: ResidentAgeNonPositiveException) => true
      case _                                        =>
    ) shouldEqual true
    (Resident(-5, Male) match
      case Left(_: ResidentAgeNonPositiveException) => true
      case _                                        =>
    ) shouldEqual true
  }

  it should "be invalid if sex is non binary" in {
    1 shouldEqual 1 // Impossible case: Sex is sealed trait
  }

  // specs on methods

  "countOldManFloors" should "return the number of men older than the specified age" in {
    val building = new Building(
      "Baker street, 221B",
      new CommercialFloor(
        Array(Commercial("Cafe")),
        ResidentialFloor(
          new Resident(13, Male),
          new Resident(37, Female),
          ResidentialFloor(
            new Resident(30, Male),
            new Resident(33, Female),
            ResidentialFloor(
              new Resident(42, Male),
              new Resident(33, Male),
              CommonAttic()
            )
          )
        )
      )
    )
    Building.countOldManFloors(building, 25) shouldEqual 2
  }

  it should "return 0 if there are no men older than the specified age" in {
    val building = new Building(
      "Baker street, 221B",
      new CommercialFloor(
        Array(Commercial("Cafe")),
        ResidentialFloor(
          new Resident(13, Male),
          new Resident(37, Female),
          ResidentialFloor(
            new Resident(20, Male),
            new Resident(33, Female),
            ResidentialFloor(
              new Resident(42, Female),
              new Resident(3, Male),
              CommonAttic()
            )
          )
        )
      )
    )
    Building.countOldManFloors(building, 25) shouldEqual 0
  }

  it should "return 0 if there are no men at all in the building" in {
    val building = new Building(
      "Baker street, 221B",
      new CommercialFloor(
        Array(Commercial("Cafe")),
        ResidentialFloor(
          new Resident(13, Female),
          new Resident(37, Female),
          ResidentialFloor(
            new Resident(20, Female),
            new Resident(33, Female),
            ResidentialFloor(
              new Resident(42, Female),
              new Resident(3, Female),
              CommonAttic()
            )
          )
        )
      )
    )
    Building.countOldManFloors(building, 25) shouldEqual 0
  }

  "womanMaxAge" should "find age of the oldest woman in the building" in {
    val building = new Building(
      "Baker street, 221B",
      new CommercialFloor(
        Array(Commercial("Cafe")),
        ResidentialFloor(
          new Resident(13, Male),
          new Resident(37, Female),
          ResidentialFloor(
            new Resident(20, Male),
            new Resident(33, Female),
            ResidentialFloor(
              new Resident(42, Female),
              new Resident(45, Female),
              CommonAttic()
            )
          )
        )
      )
    )
    Building.womanMaxAge(building) shouldEqual Some(45)
  }

  it should "return None if there are no women in the building" in {
    val building = new Building(
      "Baker street, 221B",
      new CommercialFloor(
        Array(Commercial("Cafe")),
        ResidentialFloor(
          new Resident(13, Male),
          new Resident(37, Male),
          ResidentialFloor(
            new Resident(20, Male),
            new Resident(33, Male),
            ResidentialFloor(
              new Resident(42, Male),
              new Resident(5, Male),
              CommonAttic()
            )
          )
        )
      )
    )
    Building.womanMaxAge(building) shouldEqual None
  }

  "countCommercial" should "return number of commercial establishments in the building" in {
    val building = new Building(
      "Baker street, 221B",
      new CommercialFloor(
        Array(Commercial("Cafe")),
        ResidentialFloor(
          new Resident(13, Female),
          new Resident(37, Female),
          new CommercialFloor(
            Array(Commercial("Supermarket"), Commercial("Cinema"), Commercial("Drug store")),
            new CommercialFloor(
              Array(Commercial("Restaurant"), Commercial("Cafe")),
              CommercialAttic(Commercial("Restaurant"))
            )
          )
        )
      )
    )
    Building.countCommercial(building) shouldEqual 7
  }

  it should "return 0 if there are no commercial establishments in the building" in {
    val building = new Building(
      "Baker street, 221B",
      ResidentialFloor(
        new Resident(13, Female),
        new Resident(37, Female),
        ResidentialFloor(
          new Resident(20, Female),
          new Resident(33, Female),
          ResidentialFloor(
            new Resident(42, Female),
            new Resident(3, Female),
            CommonAttic()
          )
        )
      )
    )
    Building.countCommercial(building) shouldEqual 0
  }

  "countCommercialAvg" should "return average commercial establishments through array of buildings" in {
    val buildings: Array[Building] = Array(
      new Building(
        "Baker street, 221B",
        new CommercialFloor(
          Array(Commercial("Cafe")),
          ResidentialFloor(
            new Resident(13, Male),
            new Resident(37, Male),
            ResidentialFloor(
              new Resident(20, Male),
              new Resident(33, Male),
              ResidentialFloor(
                new Resident(42, Male),
                new Resident(3, Male),
                CommonAttic()
              )
            )
          )
        )
      ),
      new Building(
        "Baker street, 221B",
        new CommercialFloor(
          Array(Commercial("Cafe")),
          ResidentialFloor(
            new Resident(13, Female),
            new Resident(37, Female),
            new CommercialFloor(
              Array(Commercial("Supermarket"), Commercial("Drug store")),
              new CommercialFloor(
                Array(Commercial("Restaurant"), Commercial("Cafe")),
                CommercialAttic(Commercial("Restaurant"))
              )
            )
          )
        )
      )
    )
    Building.countCommercialAvg(buildings) shouldEqual 3.5
  }

  it should "return 0 if there are no commercial establishments in the buildings" in {
    val buildings: Array[Building] = Array(
      new Building(
        "Baker street, 221B",
        ResidentialFloor(
          new Resident(13, Female),
          new Resident(37, Female),
          ResidentialFloor(
            new Resident(20, Female),
            new Resident(33, Female),
            ResidentialFloor(
              new Resident(42, Female),
              new Resident(3, Female),
              CommonAttic()
            )
          )
        )
      ),
      new Building(
        "Baker street, 221B",
        ResidentialFloor(
          new Resident(13, Female),
          new Resident(37, Female),
          CommonAttic()
        )
      )
    )
    Building.countCommercialAvg(buildings) shouldEqual 0.0
  }

  it should "return 0 if there are no buildings" in {
    val buildings: Array[Building] = Array()
    Building.countCommercialAvg(buildings) shouldEqual 0.0
  }

  "evenFloorsMenAvg" should "return average count of men on even floors in the building" in {
    val building = new Building(
      "Baker street, 221B",
      new CommercialFloor(
        Array(Commercial("Cafe")),
        ResidentialFloor(
          new Resident(13, Female),
          new Resident(37, Male),
          ResidentialFloor(
            new Resident(20, Male),
            new Resident(33, Male),
            ResidentialFloor(
              new Resident(42, Male),
              new Resident(3, Male),
              CommonAttic()
            )
          )
        )
      )
    )
    Building.evenFloorsMenAvg(building) shouldEqual 1.5
  }

  it should "return 0 if there are no men in the building" in {
    val building = new Building(
      "Baker street, 221B",
      new CommercialFloor(
        Array(Commercial("Cafe")),
        ResidentialFloor(
          new Resident(13, Female),
          new Resident(37, Female),
          ResidentialFloor(
            new Resident(20, Female),
            new Resident(33, Female),
            ResidentialFloor(
              new Resident(42, Female),
              new Resident(3, Female),
              CommonAttic()
            )
          )
        )
      )
    )
    Building.evenFloorsMenAvg(building) shouldEqual 0.0
  }

}
