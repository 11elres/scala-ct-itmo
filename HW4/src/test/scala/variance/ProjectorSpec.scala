package variance

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProjectorSpec extends AnyFlatSpec with Matchers {
  "Projector[RedactedWordLine]" should "take Converter[RedactedWordLine]" in {
    assertCompiles(
      "val projector: Projector[RedactedWordLine] = Projector(RedactedLineConverter)"
    )
  }

  it should "take Converter[WordLine]" in {
    assertCompiles(
      "val projector: Projector[RedactedWordLine] = Projector(LineConverter)"
    )
  }

  "Projector[WordLine]" should "not take Converter[RedactedWordLine]" in {
    assertTypeError(
      "val projector: Projector[WordLine] = Projector(RedactedLineConverter)"
    )
  }

  it should "take Converter[WordLine]" in {
    assertCompiles(
      "val projector1: Projector[WordLine] = Projector(LineConverter)"
    )
  }

  "Projector[RedactedWordLine].project" should "not take Slide[WordLine]" in {
    assertTypeError(
      "Projector[RedactedWordLine](RedactedLineConverter).project(HelloSlide[WordLine](List()))"
    )
  }

  it should "take Slide[RedactedWordLine]" in {
    Projector[RedactedWordLine](LineConverter).project(
      HelloSlide[RedactedWordLine](
        List(
          RedactedWordLine(1.0, "Hello"),
          RedactedWordLine(0.0, "World")
        )
      )
    ) shouldEqual "Hello\nWorld\n"

    Projector[RedactedWordLine](RedactedLineConverter).project(
      HelloSlide[RedactedWordLine](
        List(
          RedactedWordLine(1.0, "Hello"),
          RedactedWordLine(0.0, "World")
        )
      )
    ) shouldEqual "\u2588" * 5 + "\nWorld\n"
  }

  "Projector[WordLine].project" should "take Slide[RedactedWordLine]" in {
    Projector[WordLine](LineConverter).project(
      HelloSlide[RedactedWordLine](
        List(
          RedactedWordLine(1.0, "Hello"),
          RedactedWordLine(0.0, "World")
        )
      )
    ) shouldEqual "Hello\nWorld\n"
  }

  it should "take Slide[WordLine]" in {
    Projector[WordLine](LineConverter).project(
      HelloSlide[WordLine](
        List(
          RedactedWordLine(1.0, "Hello"),
          WordLine("World")
        )
      )
    ) shouldEqual "Hello\nWorld\n"
  }
}
