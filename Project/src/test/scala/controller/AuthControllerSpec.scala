package controller

import cats.data.EitherT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all._
import domain.authentication._
import domain.errors.{AppError, InvalidCredentialsError}
import domain.role.OrdinaryUserRole
import domain.user._
import io.circe.syntax.EncoderOps
import org.mockito.MockitoSugar._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import service.{AuthService, TokenService}
import sttp.client3._
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.integ.cats.effect.CatsMonadError

class AuthControllerSpec extends AnyFlatSpec with Matchers {

  val claims          = UserClaims(UserId(42), UserName("name"), Set(OrdinaryUserRole))
  val token           = JWTToken("token")
  val error: AppError = InvalidCredentialsError()
  val loginRequest    = LoginRequest(UserName("name"), UserPassword("pass"))

  val registerRequest = RegisterRequest(
    UserInfo(UserName("name"), UserEmail("email@email.com"), UserStatus(""), UserAbout("")),
    UserPassword("pass")
  )

  val mockTokenService: TokenService[IO] = mock[TokenService[IO]]

  when(mockTokenService.jwtEncode(claims))
    .thenReturn(IO.pure(token))

  val mockAuthService = mock[AuthService[IO]]

  val controller = AuthController.make(mockAuthService, mockTokenService)

  val backendStub: SttpBackend[IO, Any] =
    TapirStubInterpreter(SttpBackendStub[IO, Any](new CatsMonadError[IO]))
      .whenServerEndpointsRunLogic(controller.all)
      .backend()

  "loginUser" should "return token by accepted login request" in {
    reset(mockAuthService)
    when(mockAuthService.loginUser(loginRequest))
      .thenReturn(EitherT(IO.pure(claims.asRight)))

    val response = basicRequest
      .post(uri"http://test.com/login")
      .contentType("application/json")
      .body(loginRequest.asJson.noSpaces)
      .send(backendStub)

    response.unsafeRunSync() should beTheSameJson(token.asJson)
  }

  it should "return error by not accepted login request" in {
    reset(mockAuthService)
    when(mockAuthService.loginUser(loginRequest))
      .thenReturn(EitherT(IO.pure(error.asLeft)))

    val response = basicRequest
      .post(uri"http://test.com/login")
      .contentType("application/json")
      .body(loginRequest.asJson.noSpaces)
      .send(backendStub)

    response.unsafeRunSync() should beTheSameJson(error.asJson)
  }

  "registerUser" should "return token by register request json" in {
    reset(mockAuthService)
    when(mockAuthService.registerUser(registerRequest))
      .thenReturn(EitherT(IO.pure(claims.asRight)))

    val response = basicRequest
      .post(uri"http://test.com/signup")
      .contentType("application/json")
      .body(registerRequest.asJson.noSpaces)
      .send(backendStub)

    response.unsafeRunSync() should beTheSameJson(token.asJson)
  }

  it should "return error by not accepted register request" in {
    reset(mockAuthService)
    when(mockAuthService.registerUser(registerRequest))
      .thenReturn(EitherT(IO.pure(error.asLeft)))

    val response = basicRequest
      .post(uri"http://test.com/signup")
      .contentType("application/json")
      .body(registerRequest.asJson.noSpaces)
      .send(backendStub)

    response.unsafeRunSync() should beTheSameJson(error.asJson)
  }

}
