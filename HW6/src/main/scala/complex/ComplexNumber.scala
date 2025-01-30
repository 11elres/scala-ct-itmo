package complex

import scala.language.implicitConversions
import scala.math.{sqrt, atan2}

// DO NOT CHANGE ANYTHING BELOW
final case class ComplexNumber(real: Double, imaginary: Double) {
  def *(other: ComplexNumber) =
    ComplexNumber(
      (real * other.real) - (imaginary * other.imaginary),
      (real * other.imaginary) + (imaginary * other.real)
    )
  def +(other: ComplexNumber) =
    ComplexNumber(real + other.real, imaginary + other.imaginary)
  def ~=(o: ComplexNumber) =
    (real - o.real).abs < 1e-6 && (imaginary - o.imaginary).abs < 1e-6
}

object ComplexNumber {
  // DO NOT CHANGE ANYTHING ABOVE

  given [T: Numeric]: Conversion[T, ComplexNumber] with
    def apply(x: T): ComplexNumber = ComplexNumber(summon[Numeric[T]].toDouble(x), 0)

  extension (a: ComplexNumber)
    def -(b: ComplexNumber): ComplexNumber =
      ComplexNumber(a.real - b.real, a.imaginary - b.imaginary)
    def /(b: ComplexNumber): ComplexNumber =
      val denominator = b.real * b.real + b.imaginary * b.imaginary
      ComplexNumber(
        (a.real * b.real + a.imaginary * b.imaginary) / denominator,
        (a.imaginary * b.real - a.real * b.imaginary) / denominator
      )
    def polar: PolarComplexNumber =
      PolarComplexNumber(
        sqrt(a.real * a.real + a.imaginary * a.imaginary),
        atan2(a.imaginary, a.real)
      )

  extension [T: Numeric](a: T)
    def i: ComplexNumber = ComplexNumber(0, summon[Numeric[T]].toDouble(a))
    def +(b: ComplexNumber): ComplexNumber = summon[Conversion[T, ComplexNumber]](a) + b
    def -(b: ComplexNumber): ComplexNumber = summon[Conversion[T, ComplexNumber]](a) - b
    def *(b: ComplexNumber): ComplexNumber = summon[Conversion[T, ComplexNumber]](a) * b
    def /(b: ComplexNumber): ComplexNumber = summon[Conversion[T, ComplexNumber]](a) / b
    def ~=(b: ComplexNumber) = summon[Conversion[T, ComplexNumber]](a) ~= b
}

final case class PolarComplexNumber(modulus: Double, argument: Double)

object PolarComplexNumber {
  given Conversion[ComplexNumber, PolarComplexNumber] = _.polar
}

@main def demonstrate(): Unit = {
  import complex.ComplexNumber.*

  val a = 2 + 3.i
  val b = 7 - 1.i
  val d: Double = 5.3
  val i: Int = 3

  // for demonstration
  println(s"Complex numbers: a == $a, b == $b")
  println(s"Double number d == $d, integer number == $i")
  println(s"a - b == ${a - b}")
  println(s"a / b == ${a / b}")
  println(s"a.polar == ${a.polar}")
  println(s"a + d == ${a + d}")
  println(s"a - i == ${a - i}")
  println(s"a * i == ${a * i}")
  println(s"a / d == ${a / d}")
  println(s"i + a == ${i + a}")
  println(s"d - a == ${d - a}")
  println(s"d * a == ${d * a}")
  println(s"i / a == ${i / a}")
  println(s"(1e-10 + 1e-9.i) ~= 1e-10 == ${(1e-10 + 1e-9.i) ~= 1e-10}")
  println(s"1e-10 ~= (1e-10 + 1e-9.i) == ${1e-10 ~= (1e-10 + 1e-9.i)}")
  println(s"1e-10 ~= 1e-9 == ${1e-10 ~= 1e-9}")
}
