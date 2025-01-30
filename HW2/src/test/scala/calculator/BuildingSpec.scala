package calculator

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BuildingSpec extends AnyFlatSpec with Matchers {

  "apply" should "construct Building with positive parameters" in {
    noException should be thrownBy Building.Economy(1, 2, 3, 4)
    noException should be thrownBy Building.Premium(10, 100, 10, 2)
  }

  it should "throw IllegalArgumentException with non-positive parameter" in {
    an[IllegalArgumentException] should be thrownBy Building.Economy(-1, 2, 3, 4)
    an[IllegalArgumentException] should be thrownBy Building.Premium(10, -100, -10, 2)
  }

}
