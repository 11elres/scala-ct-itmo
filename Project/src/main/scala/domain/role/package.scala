package domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import doobie.Read
import io.estatico.newtype.macros.newtype
import scala.language.implicitConversions
import tofu.logging.derivation.loggable

package object role {

  @derive(loggable, encoder, decoder)
  @newtype
  case class RoleId(value: Long)

  object RoleId {

    implicit val read: Read[RoleId] = Read[Long].map(RoleId.apply)

  }

}
