package domain.authentication

import derevo.circe.{decoder, encoder}
import derevo.derive
import domain.role.Role
import domain.user._
import sttp.tapir.derevo.schema
import tofu.logging.derivation.loggable

@derive(loggable, encoder, decoder, schema)
case class LoginRequest(
  name: UserName,
  password: UserPassword
)

@derive(loggable, encoder, decoder, schema)
case class RegisterRequest(
  userInfo: UserInfo,
  password: UserPassword
)

case class UserClaims(
  id: UserId,
  name: UserName,
  roles: Set[Role]
)

@derive(encoder, decoder, schema)
case class JWTToken(token: String)
