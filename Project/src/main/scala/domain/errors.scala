package domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import domain.authentication.JWTToken
import domain.post.PostId
import domain.role.Role
import domain.user.{UserEmail, UserId, UserName}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

object errors {

  @derive(encoder, decoder)
  sealed abstract class AppError(
    val message: String,
    val cause: Option[Throwable] = None
  )

  // auth

  @derive(encoder, decoder)
  sealed abstract class AuthError(override val message: String) extends AppError(message = message)

  @derive(encoder, decoder)
  case class InvalidTokenError(token: JWTToken)
      extends AuthError(s"Failed validation of jwt token: ${token.token}")

  @derive(encoder, decoder)
  case class ExpirationTokenError(token: JWTToken)
      extends AuthError(s"Validity period of jwt token expire: ${token.token}")

  @derive(encoder, decoder)
  case class InvalidCredentialsError() extends AuthError("Incorrect username or login")

  @derive(encoder, decoder)
  case class ValidationRegisterError(cause0: AppError, reason: String)
      extends AuthError(s"Failed validation of user information during registration: $reason")

  // validation

  @derive(encoder, decoder)
  sealed abstract class ValidationError(override val message: String)
      extends AppError(message = message)

  @derive(encoder, decoder)
  case class IncorrectUserEmailError(email: UserEmail)
      extends ValidationError(s"Incorrect email: ${email.value}.")

  @derive(encoder, decoder)
  case class IncorrectUserNameError(requirement: String)
      extends ValidationError(s"Incorrect name: $requirement.")

  @derive(encoder, decoder)
  case class WeakUserPasswordError(requirement: String)
      extends ValidationError(s"Weak password: $requirement.")

  // user

  @derive(encoder, decoder)
  case class UserEmailIsAlreadyOccupiedError(email: UserEmail)
      extends AppError(s"User with ${email.value} email already exists.")

  @derive(encoder, decoder)
  case class UserNameIsAlreadyOccupiedError(name: UserName)
      extends AppError(s"User with ${name.value} name already exists.")

  @derive(encoder, decoder)
  case class UserNotFoundError(id: UserId) extends AppError(s"User with ${id.value} id not found.")

  @derive(encoder, decoder)
  case class UserByNameNotFoundError(name: UserName)
      extends AppError(s"User with ${name.value} name not found.")

  // role

  @derive(encoder, decoder)
  case class RoleAlreadyAssignedError(userId: UserId, role: Role)
      extends AppError(s"User with ${userId.value} id already have $role role.")

  @derive(encoder, decoder)
  case class RoleNotAssignedError(userId: UserId, role: Role)
      extends AppError(s"User with ${userId.value} id haven't $role role.")

  // post

  @derive(encoder, decoder)
  case class PostNotFoundError(id: PostId) extends AppError(s"Post with ${id.value} id not found.")

  // other

  @derive(encoder, decoder)
  case class PermissionDeniedError(event: String) extends AppError(s"Permission denied: $event")

  @derive(encoder, decoder)
  case class AppInternalError(cause0: Throwable) extends AppError("Internal error.", Some(cause0))

  @derive(encoder, decoder)
  case class DecodedError(override val message: String) extends AppError(message = message)

  implicit val throwableEncoder: Encoder[Throwable] =
    Encoder.encodeString.contramap(_.getMessage)

  implicit val throwableDecoder: Decoder[Throwable] =
    Decoder.decodeString.map(new Throwable(_))

  implicit val schema: Schema[AppError] =
    Schema.schemaForString.map[AppError](str => Some(DecodedError(str)))(
      _.message
    )

}
