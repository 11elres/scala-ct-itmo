package controller

import cats.Monad
import cats.data.EitherT.liftF
import domain.authentication.JWTToken
import domain.errors.AppError
import service.{AuthService, TokenService}
import sttp.tapir.server.ServerEndpoint

trait AuthController[F[_]] {

  def registerUser: ServerEndpoint[Any, F]
  def loginUser: ServerEndpoint[Any, F]
  def all: List[ServerEndpoint[Any, F]]

}

object AuthController {

  def make[F[_]: Monad, JA](
    authService: AuthService[F],
    tokenService: TokenService[F]
  ): AuthController[F] = new AuthController[F] {

    override val registerUser: ServerEndpoint[Any, F] =
      endpoints.signup.serverLogic(registerRequest =>
        (for {
          claims <- authService.registerUser(registerRequest)
          token  <- liftF[F, AppError, JWTToken](tokenService.jwtEncode(claims))
        } yield token).value
      )

    override val loginUser: ServerEndpoint[Any, F] =
      endpoints.login.serverLogic(loginRequest =>
        (for {
          claims <- authService.loginUser(loginRequest)
          token  <- liftF[F, AppError, JWTToken](tokenService.jwtEncode(claims))
        } yield token).value
      )

    override val all: List[ServerEndpoint[Any, F]] =
      List(registerUser, loginUser)
  }

}
