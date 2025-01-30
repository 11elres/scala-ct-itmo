package domain.post

import derevo.circe.{decoder, encoder}
import derevo.derive
import domain.user.UserId
import sttp.tapir.derevo.schema
import tofu.logging.derivation._

@derive(loggable, encoder, decoder, schema)
case class PostInfo(
  title: PostTitle,
  content: PostContent
)

@derive(loggable, encoder, decoder, schema)
case class CreatePost(
  user: UserId,
  postInfo: PostInfo
)

@derive(loggable, encoder, decoder, schema)
case class Post(
  id: PostId,
  userId: UserId,
  postInfo: PostInfo,
  creationTime: PostCreationTime
)
