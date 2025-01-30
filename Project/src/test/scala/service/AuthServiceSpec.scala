package service

import cats.data.EitherT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all._
import domain.authentication._
import domain.errors._
import domain.role.OrdinaryUserRole
import domain.user._
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.BCrypt
import validation.UserValidator

import java.time.Instant

class AuthServiceSpec extends AnyFlatSpec with Matchers {

  val validator: UserValidator = UserValidator.make()
  val password: UserPassword   = UserPassword("password")

  val user: User = User(
    UserId(42),
    UserInfo(UserName("name"), UserEmail("email@email.com"), UserStatus(""), UserAbout("")),
    Set(OrdinaryUserRole),
    UserPasswordHash(implicitly[PasswordHasher[IO, BCrypt]].hashpw(password.value).unsafeRunSync()),
    UserCreationTime(Instant.ofEpochSecond(10))
  )

  val claims: UserClaims = UserClaims(user.id, user.userInfo.name, user.roles)

  "loginUser" should "login with valid credentials" in {
    val loginRequest = LoginRequest(user.userInfo.name, password)

    val userService = mock[UserService[IO]]
    when(userService.findByName(loginRequest.name)) thenReturn
      EitherT(IO.pure(Some(user).asRight[AppInternalError]))
    val service = AuthService.make[IO, BCrypt](userService, validator)

    service.loginUser(loginRequest).value.unsafeRunSync() shouldBe Right(claims)
  }

  it should "not login existing user with wrong password" in {
    val loginRequest = LoginRequest(user.userInfo.name, UserPassword("hehe"))

    val userService = mock[UserService[IO]]
    when(userService.findByName(loginRequest.name)) thenReturn
      EitherT(IO.pure(Some(user).asRight[AppInternalError]))
    val service = AuthService.make[IO, BCrypt](userService, validator)

    service.loginUser(loginRequest).value.unsafeRunSync() shouldBe Left(InvalidCredentialsError())
  }

  it should "not login not existing user" in {
    val loginRequest = LoginRequest(user.userInfo.name, password)

    val userService = mock[UserService[IO]]
    when(userService.findByName(loginRequest.name)) thenReturn
      EitherT(IO.pure(identity[Option[User]](None).asRight[AppInternalError]))
    val service = AuthService.make[IO, BCrypt](userService, validator)

    service.loginUser(loginRequest).value.unsafeRunSync() shouldBe Left(InvalidCredentialsError())
  }

  "registerUser" should "register new user" in {
    val registerRequest = RegisterRequest(user.userInfo, password)

    val userService = mock[UserService[IO]]
    when(userService.findByName(registerRequest.userInfo.name)) thenReturn
      EitherT(IO.pure(identity[Option[User]](None).asRight[AppInternalError]))
    when(userService.findByEmail(registerRequest.userInfo.email)) thenReturn
      EitherT(IO.pure(identity[Option[User]](None).asRight[AppInternalError]))
    when(userService.create(any[CreateUser])) thenReturn
      EitherT(IO.pure(user.asRight[AppError]))
    val service = AuthService.make[IO, BCrypt](userService, validator)

    service.registerUser(registerRequest).value.unsafeRunSync() shouldBe Right(claims)
  }

  it should "not register user with existing name" in {
    val registerRequest = RegisterRequest(user.userInfo, password)

    val userService = mock[UserService[IO]]
    when(userService.findByName(registerRequest.userInfo.name)) thenReturn
      EitherT(IO.pure(Some(user).asRight[AppInternalError]))
    when(userService.findByEmail(registerRequest.userInfo.email)) thenReturn
      EitherT(IO.pure(identity[Option[User]](None).asRight[AppInternalError]))
    when(userService.create(any[CreateUser])) thenReturn
      EitherT(IO.pure(user.asRight[AppError]))
    val service = AuthService.make[IO, BCrypt](userService, validator)

    val request = service.registerUser(registerRequest).value.unsafeRunSync()
    request shouldBe a[Left[AppError, _]]
    request.swap.toOption.get shouldBe a[ValidationRegisterError]
    request.swap.toOption.get
      .asInstanceOf[ValidationRegisterError]
      .cause0 shouldBe UserNameIsAlreadyOccupiedError(registerRequest.userInfo.name)
  }

  it should "not register user with existing email" in {
    val registerRequest = RegisterRequest(user.userInfo, password)

    val userService = mock[UserService[IO]]
    when(userService.findByName(registerRequest.userInfo.name)) thenReturn
      EitherT(IO.pure(identity[Option[User]](None).asRight[AppInternalError]))
    when(userService.findByEmail(registerRequest.userInfo.email)) thenReturn
      EitherT(IO.pure(Some(user).asRight[AppInternalError]))
    when(userService.create(any[CreateUser])) thenReturn
      EitherT(IO.pure(user.asRight[AppError]))
    val service = AuthService.make[IO, BCrypt](userService, validator)

    val request = service.registerUser(registerRequest).value.unsafeRunSync()
    request shouldBe a[Left[AppError, _]]
    request.swap.toOption.get shouldBe a[ValidationRegisterError]
    request.swap.toOption.get
      .asInstanceOf[ValidationRegisterError]
      .cause0 shouldBe UserEmailIsAlreadyOccupiedError(registerRequest.userInfo.email)
  }

  it should "validate name" in {
    val registerRequest = RegisterRequest(user.userInfo.copy(name = UserName("???")), password)

    val userService = mock[UserService[IO]]
    when(userService.findByName(registerRequest.userInfo.name)) thenReturn
      EitherT(IO.pure(identity[Option[User]](None).asRight[AppInternalError]))
    when(userService.findByEmail(registerRequest.userInfo.email)) thenReturn
      EitherT(IO.pure(identity[Option[User]](None).asRight[AppInternalError]))
    val service = AuthService.make[IO, BCrypt](userService, validator)

    service
      .registerUser(registerRequest)
      .value
      .unsafeRunSync() shouldBe a[Left[IncorrectUserNameError, _]]
  }

  it should "validate email" in {
    val registerRequest = RegisterRequest(user.userInfo.copy(email = UserEmail("???")), password)

    val userService = mock[UserService[IO]]
    when(userService.findByName(registerRequest.userInfo.name)) thenReturn
      EitherT(IO.pure(identity[Option[User]](None).asRight[AppInternalError]))
    when(userService.findByEmail(registerRequest.userInfo.email)) thenReturn
      EitherT(IO.pure(identity[Option[User]](None).asRight[AppInternalError]))
    val service = AuthService.make[IO, BCrypt](userService, validator)

    service
      .registerUser(registerRequest)
      .value
      .unsafeRunSync() shouldBe a[Left[IncorrectUserEmailError, _]]
  }

  it should "validate password" in {
    val registerRequest = RegisterRequest(user.userInfo, UserPassword("abc"))

    val userService = mock[UserService[IO]]
    when(userService.findByName(registerRequest.userInfo.name)) thenReturn
      EitherT(IO.pure(identity[Option[User]](None).asRight[AppInternalError]))
    when(userService.findByEmail(registerRequest.userInfo.email)) thenReturn
      EitherT(IO.pure(identity[Option[User]](None).asRight[AppInternalError]))
    val service = AuthService.make[IO, BCrypt](userService, validator)

    service
      .registerUser(registerRequest)
      .value
      .unsafeRunSync() shouldBe a[Left[WeakUserPasswordError, _]]
  }

}
