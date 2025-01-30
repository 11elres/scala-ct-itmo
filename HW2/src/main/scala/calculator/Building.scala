package calculator

sealed trait Building(length: Int, width: Int, height: Int, floorNumber: Int)

object Building {

  final case class Economy(length: Int, width: Int, height: Int, floorNumber: Int)
      extends Building(length, width, height, floorNumber)

  final case class Premium(length: Int, width: Int, height: Int, floorNumber: Int)
      extends Building(length, width, height, floorNumber)

  private def checkArguments[A](
    length: Int,
    width: Int,
    height: Int,
    floorNumber: Int,
    constructor: (Int, Int, Int, Int) => A
  ): A =
    if !(length > 0 && width > 0 && height > 0 && floorNumber > 0)
    then throw new IllegalArgumentException("Non positive parameter was received")
    else constructor(length, width, height, floorNumber)

  object Economy {

    def apply(length: Int, width: Int, height: Int, floorNumber: Int): Economy =
      checkArguments(length, width, height, floorNumber, new Economy(_, _, _, _))

  }

  object Premium {

    def apply(length: Int, width: Int, height: Int, floorNumber: Int): Premium =
      checkArguments(length, width, height, floorNumber, new Premium(_, _, _, _))

  }

}
