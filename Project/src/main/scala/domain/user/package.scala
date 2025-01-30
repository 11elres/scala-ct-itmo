package domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import doobie.Read
import doobie.implicits.javasql._
import io.estatico.newtype.macros.newtype
import scala.language.implicitConversions
import sttp.tapir.{Codec, CodecFormat, Schema}
import tofu.logging.derivation._

import java.sql.Timestamp
import java.time.Instant

package object user {

  @derive(loggable, encoder, decoder)
  @newtype
  case class UserId(value: Long)

  object UserId {

    implicit val read: Read[UserId] = Read[Long].map(UserId.apply)

    implicit val schema: Schema[UserId] =
      Schema.schemaForLong.map(long => Some(UserId(long)))(_.value)

  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class UserName(value: String)

  object UserName {

    implicit val read: Read[UserName] = Read[String].map(UserName.apply)

    implicit val schema: Schema[UserName] =
      Schema.schemaForString.map(string => Some(UserName(string)))(_.value)

    implicit val codec: Codec[String, UserName, CodecFormat.TextPlain] =
      Codec.string.map(UserName(_))(_.value)

  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class UserEmail(value: String)

  object UserEmail {

    implicit val read: Read[UserEmail] = Read[String].map(UserEmail.apply)

    implicit val schema: Schema[UserEmail] =
      Schema.schemaForString.map(string => Some(UserEmail(string)))(_.value)

  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class UserStatus(value: String)

  object UserStatus {

    implicit val read: Read[UserStatus] = Read[String].map(UserStatus.apply)

    implicit val schema: Schema[UserStatus] =
      Schema.schemaForString.map(string => Some(UserStatus(string)))(_.value)

  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class UserAbout(value: String)

  object UserAbout {

    implicit val read: Read[UserAbout] = Read[String].map(UserAbout.apply)

    implicit val schema: Schema[UserAbout] =
      Schema.schemaForString.map(string => Some(UserAbout(string)))(_.value)

  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class UserPassword(value: String)

  object UserPassword {

    implicit val read: Read[UserPassword] = Read[String].map(UserPassword.apply)

    implicit val schema: Schema[UserPassword] =
      Schema.schemaForString.map(string => Some(UserPassword(string)))(_.value)

  }

  @newtype
  case class UserPasswordHash(value: String)

  object UserPasswordHash {

    implicit val read: Read[UserPasswordHash] = Read[String].map(UserPasswordHash.apply)

    implicit val schema: Schema[UserPasswordHash] =
      Schema.schemaForString.map(string => Some(UserPasswordHash(string)))(_.value)

  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class UserCreationTime(value: Instant)

  object UserCreationTime {

    implicit val read: Read[UserCreationTime] =
      Read[Timestamp].map(timestamp => UserCreationTime(timestamp.toInstant))

    implicit val schema: Schema[UserCreationTime] =
      Schema.schemaForString
        .map(str => Some(UserCreationTime(Timestamp.valueOf(str).toInstant)))(userCreationTime =>
          Timestamp.from(userCreationTime.value).toString
        )

  }

}
