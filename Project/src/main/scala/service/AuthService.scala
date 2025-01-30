package service

import cats._
import cats.data.EitherT
import cats.data.EitherT._
import cats.implicits.catsSyntaxApplicativeId
import domain.authentication._
import domain.errors._
import domain.user._
import tsec.passwordhashers.{PasswordHash, PasswordHasher}
import validation.UserValidator

trait AuthService[F[_]] {

  def registerUser(
    userInfo: RegisterRequest
  ): EitherT[F, AppError, UserClaims]

  def loginUser(
    userCredentials: LoginRequest
  ): EitherT[F, AppError, UserClaims]

}

object AuthService {

  def make[F[_]: MonadThrow, HA](
    userService: UserService[F],
    validator: UserValidator
  )(implicit
    hasher: PasswordHasher[F, HA]
  ): AuthService[F] =
    new AuthService[F] {

      override def registerUser(
        registerRequest: RegisterRequest
      ): EitherT[F, AppError, UserClaims] = {
        for {
          _ <- userService.findByEmail(registerRequest.userInfo.email).flatMap {
            case Some(_) =>
              leftT[F, Unit](
                identity[AppError](
                  ValidationRegisterError(
                    UserEmailIsAlreadyOccupiedError(registerRequest.userInfo.email),
                    "email already exists"
                  )
                )
              )
            case None => ().pure[EitherT[F, AppError, *]]
          }

          _ <- userService.findByName(registerRequest.userInfo.name).flatMap {
            case Some(_) =>
              leftT[F, Unit](
                identity[AppError](
                  ValidationRegisterError(
                    UserNameIsAlreadyOccupiedError(registerRequest.userInfo.name),
                    "name already exists"
                  )
                )
              )
            case None => ().pure[EitherT[F, AppError, *]]
          }

          user <- for {
            createUser <- (for {
              name <- EitherT.fromEither[F](validator.validateName(registerRequest.userInfo.name))
              email <- EitherT.fromEither[F](
                validator.validateEmail(registerRequest.userInfo.email)
              )
              password <- EitherT.fromEither[F](
                validator.validatePassword(registerRequest.password)
              )
              passwordHash <- liftF[F, ValidationError, PasswordHash[HA]](
                hasher.hashpw(password.value)
              )
            } yield CreateUser(
              UserInfo(
                name,
                email,
                registerRequest.userInfo.status,
                registerRequest.userInfo.about
              ),
              UserPasswordHash(passwordHash)
            )).leftMap(ValidationRegisterError(_, "invalid user field"))
            user <- userService.create(createUser)
          } yield user
        } yield UserClaims(user.id, user.userInfo.name, user.roles)
      }

      override def loginUser(
        userCredentials: LoginRequest
      ): EitherT[F, AppError, UserClaims] = {
        for {
          user <- userService
            .findByName(userCredentials.name)
            .subflatMap(_.toRight(InvalidCredentialsError()))
            .leftMap[AppError](identity)

          isMatch <- liftF(
            hasher.checkpwBool(
              userCredentials.password.value,
              PasswordHash[HA](user.passwordHash.value)
            )
          )

          _ <- EitherT
            .cond[F](
              isMatch,
              (),
              InvalidCredentialsError()
            )
            .leftMap[AppError](identity)
        } yield UserClaims(user.id, user.userInfo.name, user.roles)
      }
    }

}
