package domain.role

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, Encoder}
import sttp.tapir.{Codec, CodecFormat, Schema}
import tofu.logging.derivation.loggable

sealed trait Role {
  self =>

  override def toString: String = self match {
    case OrdinaryUserRole => "user"
    case CensorRole       => "censor"
    case AdminRole        => "admin"
  }

}

object Role {

  val toRole: String => Option[Role] = {
    case "admin"  => Some(identity[Role](AdminRole))
    case "user"   => Some(OrdinaryUserRole)
    case "censor" => Some(CensorRole)
    case _        => None
  }

  implicit val encoder: Encoder[Role] = Encoder.encodeString.contramap[Role](_.toString)

  implicit val decoder: Decoder[Role] =
    Decoder.decodeString.emap(s => toRole(s).toRight(s"Unknown role $s"))

  implicit val schema: Schema[Role] =
    Schema.schemaForString.map(toRole)(_.toString)

  implicit val codec: Codec[String, Role, CodecFormat.TextPlain] =
    Codec.string.map(
      toRole(_).getOrElse(
        throw new IllegalArgumentException("Invalid role name.")
      )
    )(_.toString)

}

@derive(loggable, encoder, decoder)
case object OrdinaryUserRole extends Role

@derive(loggable, encoder, decoder)
case object CensorRole extends Role

@derive(loggable, encoder, decoder)
case object AdminRole extends Role
