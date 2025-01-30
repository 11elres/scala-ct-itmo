package controller

import cats.data.EitherT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all._
import domain.authentication._
import domain.errors._
import domain.role.{AdminRole, CensorRole, OrdinaryUserRole}
import domain.user._
import io.circe.syntax.EncoderOps
import io.circe.parser._
import org.mockito.MockitoSugar._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import service._
import sttp.client3._
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.integ.cats.effect.CatsMonadError

import java.time.Instant

class UserControllerSpec extends AnyFlatSpec with Matchers {

  val token = JWTToken("token")

  val user = User(
    UserId(42),
    UserInfo(UserName("name"), UserEmail("email@email.com"), UserStatus(""), UserAbout("")),
    Set(OrdinaryUserRole),
    UserPasswordHash("$widfhhco$uiadhcvkdsvkceiufhliwehfu"),
    UserCreationTime(Instant.now())
  )

  val userTemplate = UserTemplate(user.id, user.userInfo, user.passwordHash, user.creationTime)
  val claims       = UserClaims(user.id, user.userInfo.name, Set())

  val mockTokenService = mock[TokenService[IO]]

  val mockUserService = mock[UserService[IO]]

  val controller = UserController.make(mockUserService, mockTokenService)

  val backendStub: SttpBackend[IO, Any] =
    TapirStubInterpreter(SttpBackendStub[IO, Any](new CatsMonadError[IO]))
      .whenServerEndpointsRunLogic(controller.all)
      .backend()

  "userByName" should "return existing user" in {
    reset(mockUserService)
    when(mockUserService.findByName(user.userInfo.name))
      .thenReturn(EitherT(IO.pure(Some(user).asRight)))

    reset(mockTokenService)

    val response = basicRequest
      .get(uri"http://test.com/user/${user.userInfo.name}")
      .send(backendStub)

    response.unsafeRunSync() should beTheSameJson(user.userInfo.asJson)
  }

  it should "return error for not existing user" in {
    reset(mockUserService)
    when(mockUserService.findByName(user.userInfo.name))
      .thenReturn(EitherT(IO.pure(identity[Option[User]](None).asRight)))

    reset(mockTokenService)

    val response = basicRequest
      .get(uri"http://test.com/user/${user.userInfo.name}")
      .send(backendStub)

    val expected: AppError = UserByNameNotFoundError(user.userInfo.name)
    response.unsafeRunSync() should beTheSameJson(expected.asJson)
  }

  "updateUser" should "update own profile" in {
    reset(mockUserService)
    when(mockUserService.findByName(user.userInfo.name))
      .thenReturn(EitherT(IO.pure(Some(user).asRight)))
    when(mockUserService.update(user.id, user.userInfo))
      .thenReturn(EitherT(IO.pure(user.asRight)))

    reset(mockTokenService)
    when(mockTokenService.verifyUser(token))
      .thenReturn(EitherT(IO.pure(claims.asRight)))

    val response = basicRequest
      .patch(uri"http://test.com/user/${user.userInfo.name}")
      .header("Authorization", s"Bearer ${token.token}")
      .body(user.userInfo.asJson.noSpaces)
      .send(backendStub)

    response.unsafeRunSync() should beTheSameJson(user.userInfo.asJson)
  }

  it should "not update strange profile" in {
    reset(mockUserService)
    when(mockUserService.findByName(user.userInfo.name))
      .thenReturn(EitherT(IO.pure(Some(user).asRight)))
    when(mockUserService.update(user.id, user.userInfo))
      .thenReturn(EitherT(IO.pure(user.asRight)))

    reset(mockTokenService)
    when(mockTokenService.verifyUser(token))
      .thenReturn(
        EitherT(IO.pure(claims.copy(id = UserId(666), name = UserName("stranger")).asRight))
      )

    val response = basicRequest
      .patch(uri"http://test.com/user/${user.userInfo.name}")
      .header("Authorization", s"Bearer ${token.token}")
      .body(user.userInfo.asJson.noSpaces)
      .send(backendStub)

    decode[AppError](
      response.unsafeRunSync().body.fold(identity, identity)
    ) shouldBe a[Right[_, PermissionDeniedError]]
  }

  "deleteUser" should "delete own profile" in {
    reset(mockUserService)
    when(mockUserService.findByName(user.userInfo.name))
      .thenReturn(EitherT(IO.pure(Some(user).asRight)))
    when(mockUserService.removeById(user.id))
      .thenReturn(EitherT(IO.pure(().asRight)))

    reset(mockTokenService)
    when(mockTokenService.verifyUser(token))
      .thenReturn(EitherT(IO.pure(claims.asRight)))

    val response = basicRequest
      .delete(uri"http://test.com/user/${user.userInfo.name}")
      .header("Authorization", s"Bearer ${token.token}")
      .send(backendStub)

    response.unsafeRunSync().body.isRight shouldBe true
  }

  it should "delete any profile with admin rules" in {
    reset(mockUserService)
    when(mockUserService.findByName(user.userInfo.name))
      .thenReturn(EitherT(IO.pure(Some(user).asRight)))
    when(mockUserService.removeById(user.id))
      .thenReturn(EitherT(IO.pure(().asRight)))

    reset(mockTokenService)
    when(mockTokenService.verifyUser(token))
      .thenReturn(
        EitherT(
          IO.pure(
            claims.copy(id = UserId(999), name = UserName("admin"), roles = Set(AdminRole)).asRight
          )
        )
      )

    val response = basicRequest
      .delete(uri"http://test.com/user/${user.userInfo.name}")
      .header("Authorization", s"Bearer ${token.token}")
      .send(backendStub)

    response.unsafeRunSync().body.isRight shouldBe true
  }

  it should "not delete any profile without admin rules" in {
    reset(mockUserService)
    when(mockUserService.findByName(user.userInfo.name))
      .thenReturn(EitherT(IO.pure(Some(user).asRight)))
    when(mockUserService.removeById(user.id))
      .thenReturn(EitherT(IO.pure(().asRight)))

    reset(mockTokenService)
    when(mockTokenService.verifyUser(token))
      .thenReturn(
        EitherT(IO.pure(claims.copy(id = UserId(999), name = UserName("not_admin")).asRight))
      )

    val response = basicRequest
      .delete(uri"http://test.com/user/${user.userInfo.name}")
      .header("Authorization", s"Bearer ${token.token}")
      .send(backendStub)

    decode[AppError](
      response.unsafeRunSync().body.fold(identity, identity)
    ) shouldBe a[Right[_, PermissionDeniedError]]
  }

  "listUsers" should "list users" in {
    reset(mockUserService)
    when(mockUserService.listAll())
      .thenReturn(EitherT(IO.pure(List(userTemplate).asRight)))

    reset(mockTokenService)

    val response = basicRequest
      .get(uri"http://test.com/users")
      .send(backendStub)

    response.unsafeRunSync() should beTheSameJson(List(user.userInfo).asJson)
  }

  "assignRole" should "be able to assign role if we are admin" in {
    reset(mockUserService)
    when(mockUserService.findByName(user.userInfo.name))
      .thenReturn(EitherT(IO.pure(Some(user).asRight)))
    when(mockUserService.assignRoleToUser(user.id, CensorRole))
      .thenReturn(EitherT(IO.pure(().asRight)))

    reset(mockTokenService)
    when(mockTokenService.verifyUser(token))
      .thenReturn(
        EitherT(
          IO.pure(
            claims.copy(id = UserId(999), name = UserName("admin"), roles = Set(AdminRole)).asRight
          )
        )
      )

    val response = basicRequest
      .post(uri"http://test.com/user/${user.userInfo.name}/role?role=censor")
      .header("Authorization", s"Bearer ${token.token}")
      .send(backendStub)

    response.unsafeRunSync().body.isRight shouldBe true
  }

  it should "not be able to assign role if we aren't admin" in {
    reset(mockUserService)
    when(mockUserService.findByName(user.userInfo.name))
      .thenReturn(EitherT(IO.pure(Some(user.copy(roles = Set(AdminRole))).asRight)))
    when(mockUserService.assignRoleToUser(user.id, CensorRole))
      .thenReturn(EitherT(IO.pure(().asRight)))

    reset(mockTokenService)
    when(mockTokenService.verifyUser(token))
      .thenReturn(
        EitherT(IO.pure(claims.copy(id = UserId(999), name = UserName("not_admin")).asRight))
      )

    val response = basicRequest
      .post(uri"http://test.com/user/${user.userInfo.name}/role?role=censor")
      .header("Authorization", s"Bearer ${token.token}")
      .send(backendStub)

    decode[AppError](
      response.unsafeRunSync().body.fold(identity, identity)
    ) shouldBe a[Right[_, PermissionDeniedError]]
  }

  "deleteRole" should "be able to delete role if we are admin" in {
    reset(mockUserService)
    when(mockUserService.findByName(user.userInfo.name))
      .thenReturn(EitherT(IO.pure(Some(user).asRight)))
    when(mockUserService.removeRoleFromUser(user.id, CensorRole))
      .thenReturn(EitherT(IO.pure(().asRight)))

    reset(mockTokenService)
    when(mockTokenService.verifyUser(token))
      .thenReturn(
        EitherT(
          IO.pure(
            claims.copy(id = UserId(999), name = UserName("admin"), roles = Set(AdminRole)).asRight
          )
        )
      )

    val response = basicRequest
      .delete(uri"http://test.com/user/${user.userInfo.name}/role?role=censor")
      .header("Authorization", s"Bearer ${token.token}")
      .send(backendStub)
    response.unsafeRunSync().body.isRight shouldBe true
  }

  it should "not be able to delete role if we aren't admin" in {
    reset(mockUserService)
    when(mockUserService.findByName(user.userInfo.name))
      .thenReturn(EitherT(IO.pure(Some(user.copy(roles = Set(AdminRole))).asRight)))
    when(mockUserService.removeRoleFromUser(user.id, CensorRole))
      .thenReturn(EitherT(IO.pure(().asRight)))

    reset(mockTokenService)
    when(mockTokenService.verifyUser(token))
      .thenReturn(
        EitherT(IO.pure(claims.copy(id = UserId(999), name = UserName("not_admin")).asRight))
      )

    val response = basicRequest
      .delete(uri"http://test.com/user/${user.userInfo.name}/role?role=censor")
      .header("Authorization", s"Bearer ${token.token}")
      .send(backendStub)

    decode[AppError](
      response.unsafeRunSync().body.fold(identity, identity)
    ) shouldBe a[Right[_, PermissionDeniedError]]
  }

}
