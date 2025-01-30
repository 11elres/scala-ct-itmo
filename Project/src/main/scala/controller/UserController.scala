package controller

import cats.data.EitherT
import cats.effect.IO
import domain.authentication.UserClaims
import domain.errors._
import domain.role._
import domain.user.User
import service.{TokenService, UserService}
import sttp.tapir.server.ServerEndpoint

trait UserController[F[_]] {

  def userByName: ServerEndpoint[Any, F]
  def updateUser: ServerEndpoint[Any, F]
  def deleteUser: ServerEndpoint[Any, F]
  def listUsers: ServerEndpoint[Any, F]
  def assignRole: ServerEndpoint[Any, F]
  def deleteRole: ServerEndpoint[Any, F]
  def all: List[ServerEndpoint[Any, F]]

}

object UserController {

  def make(
    userService: UserService[IO],
    tokenService: TokenService[IO]
  ): UserController[IO] = new UserController[IO] {

    override val userByName: ServerEndpoint[Any, IO] =
      endpoints.userByName
        .serverLogic(name =>
          (for {
            userOpt <- userService.findByName(name)
            user <- EitherT
              .fromEither[IO](userOpt.toRight(UserByNameNotFoundError(name)))
              .leftMap(identity[AppError])
          } yield user.userInfo).value
        )

    override val updateUser: ServerEndpoint[Any, IO] =
      endpoints.updateUser
        .serverSecurityLogic[UserClaims, IO](tokenService.verifyUser(_).value)
        .serverLogic(claims =>
          nameAndInfo =>
            (for {
              user <- userService
                .findByName(nameAndInfo._1)
                .flatMap(opt =>
                  EitherT
                    .fromEither[IO](opt.toRight(UserByNameNotFoundError(nameAndInfo._1)))
                    .leftMap(identity[AppError])
                )

              updatedUser <-
                if (claims.id == user.id)
                  userService.update(user.id, nameAndInfo._2)
                else
                  EitherT
                    .leftT[IO, User](
                      PermissionDeniedError(
                        s"Updating user: User with ${claims.id.value} tries to update info of user with ${user.id.value} id"
                      )
                    )
                    .leftMap(identity[AppError])
            } yield updatedUser.userInfo).value
        )

    override val deleteUser: ServerEndpoint[Any, IO] =
      endpoints.deleteUser
        .serverSecurityLogic[UserClaims, IO](tokenService.verifyUser(_).value)
        .serverLogic(claims =>
          name =>
            (for {
              user <- userService
                .findByName(name)
                .flatMap(opt =>
                  EitherT
                    .fromEither[IO](opt.toRight(UserByNameNotFoundError(name)))
                    .leftMap(identity[AppError])
                )

              _ <-
                if (
                  claims.id == user.id ||
                  claims.roles.contains(AdminRole)
                )
                  userService.removeById(user.id)
                else
                  EitherT
                    .leftT[IO, User](
                      PermissionDeniedError(
                        s"Deleting user: User with ${claims.id.value} tries to delete user with ${user.id.value} id"
                      )
                    )
                    .leftMap(identity[AppError])
            } yield ()).value
        )

    override val listUsers: ServerEndpoint[Any, IO] =
      endpoints.listUsers
        .serverLogic(_ =>
          userService.listAll().map(_.map(_.userInfo)).leftMap(identity[AppError]).value
        )

    override val assignRole: ServerEndpoint[Any, IO] =
      endpoints.assignRole
        .serverSecurityLogic[UserClaims, IO](tokenService.verifyUser(_).value)
        .serverLogic(claims =>
          nameAndRole =>
            (for {
              userOpt <- userService.findByName(nameAndRole._1)
              user <- EitherT
                .fromEither[IO](userOpt.toRight(UserByNameNotFoundError(nameAndRole._1)))
                .leftMap(identity[AppError])

              _ <-
                if (claims.roles.contains(AdminRole))
                  userService.assignRoleToUser(user.id, nameAndRole._2)
                else
                  EitherT
                    .leftT[IO, Unit](
                      PermissionDeniedError(
                        s"User with ${claims.id.value} id isn't admin and tries to edit roles"
                      )
                    )
                    .leftMap(identity[AppError])
            } yield ()).value
        )

    override val deleteRole: ServerEndpoint[Any, IO] =
      endpoints.deleteRole
        .serverSecurityLogic[UserClaims, IO](tokenService.verifyUser(_).value)
        .serverLogic(claims =>
          nameAndRole =>
            (for {
              userOpt <- userService.findByName(nameAndRole._1)
              user <- EitherT
                .fromEither[IO](userOpt.toRight(UserByNameNotFoundError(nameAndRole._1)))
                .leftMap(identity[AppError])

              _ <-
                if (claims.roles.contains(AdminRole))
                  userService.removeRoleFromUser(user.id, nameAndRole._2)
                else
                  EitherT
                    .leftT[IO, Unit](
                      PermissionDeniedError(
                        s"User with ${claims.id.value} id isn't admin and tries to edit roles"
                      )
                    )
                    .leftMap(identity[AppError])
            } yield ()).value
        )

    override val all: List[ServerEndpoint[Any, IO]] =
      List(userByName, updateUser, deleteUser, listUsers, assignRole, deleteRole)
  }

}
