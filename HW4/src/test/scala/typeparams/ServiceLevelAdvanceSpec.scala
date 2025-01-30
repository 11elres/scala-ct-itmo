package typeparams

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ServiceLevelAdvanceSpec extends AnyFlatSpec with Matchers {
  "advance" should "upgrade step by step the level from the Economy to each leaf class" in {
    assertCompiles(
      "val economy: ServiceLevelAdvance[Economy] = ServiceLevelAdvance()\n" +
        "val upgradedEconomy: ServiceLevelAdvance[UpgradedEconomy] = economy.advance\n" +
        "val special1b: ServiceLevelAdvance[Special1b] = upgradedEconomy.advance"
    )
    assertCompiles(
      "val economy: ServiceLevelAdvance[Economy] = ServiceLevelAdvance()\n" +
        "val extendedEconomy: ServiceLevelAdvance[ExtendedEconomy] = economy.advance\n" +
        "val business: ServiceLevelAdvance[Business] = extendedEconomy.advance\n" +
        "val elite: ServiceLevelAdvance[Elite] = business.advance"
    )
    assertCompiles(
      "val economy: ServiceLevelAdvance[Economy] = ServiceLevelAdvance()\n" +
        "val extendedEconomy: ServiceLevelAdvance[ExtendedEconomy] = economy.advance\n" +
        "val business: ServiceLevelAdvance[Business] = extendedEconomy.advance\n" +
        "val platinum: ServiceLevelAdvance[Platinum] = business.advance"
    )
  }

  it should "upgrade through several levels" in {
    assertCompiles(
      "val economy: ServiceLevelAdvance[Economy] = ServiceLevelAdvance()\n" +
        "val business: ServiceLevelAdvance[Business] = economy.advance"
    )
    assertCompiles(
      "val extendedEconomy: ServiceLevelAdvance[ExtendedEconomy] = ServiceLevelAdvance()\n" +
        "val elite: ServiceLevelAdvance[Elite] = extendedEconomy.advance"
    )
  }

  it should "detect type error with downgrading" in {
    assertTypeError(
      "val economy: ServiceLevelAdvance[Economy] = ServiceLevelAdvance[Business]().advance"
    )
    assertTypeError(
      "val extendedEconomy: ServiceLevelAdvance[ExtendedEconomy] = ServiceLevelAdvance[Business]().advance"
    )
    assertTypeError(
      "val economy: ServiceLevelAdvance[Economy] = ServiceLevelAdvance[Special1b]().advance"
    )
  }
}
