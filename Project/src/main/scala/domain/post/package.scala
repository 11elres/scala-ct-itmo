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

package object post {

  @derive(loggable, encoder, decoder)
  @newtype
  case class PostId(value: Long)

  object PostId {

    implicit val read: Read[PostId] = Read[Long].map(PostId.apply)

    implicit val schema: Schema[PostId] =
      Schema.schemaForLong.map(long => Some(PostId(long)))(_.value)

    implicit val codec: Codec[String, PostId, CodecFormat.TextPlain] =
      Codec.long.map(PostId(_))(_.value)

  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class PostTitle(value: String)

  object PostTitle {

    implicit val read: Read[PostTitle] = Read[String].map(PostTitle.apply)

    implicit val schema: Schema[PostTitle] =
      Schema.schemaForString.map(string => Some(PostTitle(string)))(_.value)

    implicit val codec: Codec[String, PostTitle, CodecFormat.TextPlain] =
      Codec.string.map(PostTitle(_))(_.value)

  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class PostContent(value: String)

  object PostContent {

    implicit val read: Read[PostContent] = Read[String].map(PostContent.apply)

    implicit val schema: Schema[PostContent] =
      Schema.schemaForString.map(string => Some(PostContent(string)))(_.value)

  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class PostCreationTime(value: Instant)

  object PostCreationTime {

    implicit val read: Read[PostCreationTime] =
      Read[Timestamp].map(timestamp => PostCreationTime(timestamp.toInstant))

    implicit val schema: Schema[PostCreationTime] =
      Schema.schemaForString
        .map(str => Some(PostCreationTime(Timestamp.valueOf(str).toInstant)))(userCreationTime =>
          Timestamp.from(userCreationTime.value).toString
        )

  }

}
