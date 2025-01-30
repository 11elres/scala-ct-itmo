package building

object Errors {

  class IncorrectBuildingContentException extends Exception

  class ResidentAgeNonPositiveException extends Exception

  class CommercialFloorWithoutCommercialsException extends Exception

}
