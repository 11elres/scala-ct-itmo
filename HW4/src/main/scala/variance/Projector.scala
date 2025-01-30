package variance

import scala.annotation.tailrec

trait Converter[-S] {
  def convert(value: S): String
}

trait Slide[R] {
  def read: (Option[R], Slide[R])
}

class Projector[R](converter: Converter[R]) {
  def project[R1 <: R](screen: Slide[R1]): String =
    @tailrec
    def helpProject(screen: Slide[R1], acc: String): String =
      screen.read match
        case (None, _)                => acc
        case (Some(value), nextSlide) => helpProject(nextSlide, acc + converter.convert(value))
    helpProject(screen, "")
}

class WordLine(val word: String)

class RedactedWordLine(val redactionFactor: Double, word: String) extends WordLine(word)

object LineConverter extends Converter[WordLine] {
  override def convert(value: WordLine): String = value.word + "\n"
}

object RedactedLineConverter extends Converter[RedactedWordLine] {
  private val randomizer = new scala.util.Random
  override def convert(value: RedactedWordLine): String =
    if (randomizer.nextDouble() <= value.redactionFactor)
      "\u2588" * value.word.length + '\n'
    else value.word + '\n'
}

class HelloSlide[R <: WordLine] private (lines: Seq[R], index: Int) extends Slide[R] {
  def this(lines: Seq[R]) = this(lines, 0)
  override def read: (Option[R], Slide[R]) =
    if (index == lines.length) (None, HelloSlide(Seq.empty))
    else (Some(lines(index)), HelloSlide(lines, index + 1))
}
