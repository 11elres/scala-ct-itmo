package domain.user

import derevo.circe.{decoder, encoder}
import derevo.derive
import domain.role.Role
import sttp.tapir.derevo.schema
import tofu.logging.derivation._

@derive(loggable, encoder, decoder, schema)
case class UserInfo(
  name: UserName,
  email: UserEmail,
  status: UserStatus,
  about: UserAbout
)

case class CreateUser(
  userInfo: UserInfo,
  passwordHash: UserPasswordHash
)

case class UserTemplate(
  id: UserId,
  userInfo: UserInfo,
  passwordHash: UserPasswordHash,
  creationTime: UserCreationTime
) {

  def toUser(roles: Set[Role]): User =
    User(
      id = id,
      userInfo = userInfo,
      roles = roles,
      passwordHash = passwordHash,
      creationTime = creationTime
    )

}

case class User(
  id: UserId,
  userInfo: UserInfo,
  roles: Set[Role],
  passwordHash: UserPasswordHash,
  creationTime: UserCreationTime
)
