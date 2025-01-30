package unmarshal.encoder

import cats.kernel.Semigroup
import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Witness}
import unmarshal.model.Json
import unmarshal.model.Json.{
  JsonArray,
  JsonBool,
  JsonDouble,
  JsonNull,
  JsonNum,
  JsonObject,
  JsonString,
  semigroup
}

trait Encoder[A] {
  def toJson(value: A): Json
}

object Encoder {

  def apply[A](implicit
    encoder: Encoder[A]
  ): Encoder[A] = encoder

  protected trait LocalEncoder[A] {
    def toJson(value: A): Json
  }

  protected object LocalEncoder {

    implicit def boolEncoder: LocalEncoder[Boolean] = JsonBool(_)

    implicit def longEncoder: LocalEncoder[Long] = JsonNum(_)

    implicit def doubleEncoder: LocalEncoder[Double] = JsonDouble(_)

    implicit def stringEncoder: LocalEncoder[String] = JsonString(_)

    implicit def optionEncoder[A](implicit
      encoder: Lazy[LocalEncoder[A]]
    ): LocalEncoder[Option[A]] = {
      case None        => JsonNull
      case Some(value) => encoder.value.toJson(value)
    }

    implicit def listEncoder[A](implicit
      encoder: Lazy[LocalEncoder[A]]
    ): LocalEncoder[List[A]] = { list => JsonArray(list.map(encoder.value.toJson)) }

    trait HListEncoder[A] {
      def toJson(value: A): JsonObject
    }

    implicit val hNilEncoder: HListEncoder[HNil] = { _ => JsonObject(Map()) }

    implicit def hListEncoder[K <: Symbol, V, T <: HList](implicit
      witness: Witness.Aux[K],
      hEncoder: Lazy[LocalEncoder[V]],
      tEncoder: Lazy[HListEncoder[T]]
    ): HListEncoder[FieldType[K, V] :: T] = { case h :: t =>
      Semigroup[JsonObject].combine(
        JsonObject(Map(witness.value.name -> hEncoder.value.toJson(h))),
        tEncoder.value.toJson(t)
      )
    }

    implicit def defaultEncoder[A, R <: HList](implicit
      lgen: LabelledGeneric[A] { type Repr = R },
      encoder: Lazy[HListEncoder[R]]
    ): LocalEncoder[A] = { value => encoder.value.toJson(lgen.to(value)) }

  }

  def autoDerive[A](implicit
    encoder: LocalEncoder[A]
  ): Encoder[A] = encoder.toJson

}
