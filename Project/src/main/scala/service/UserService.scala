package service

import cats.data.EitherT
import cats.effect.MonadCancelThrow
import dao.{RoleRepository, UserRepository}
import domain.errors._
import domain.role.Role
import domain.user._
import doobie._
import doobie.implicits._

trait UserService[F[_]] {

  def create(user: CreateUser): EitherT[F, AppError, User]
  def update(userId: UserId, newUserInfo: UserInfo): EitherT[F, AppError, User]
  def removeById(id: UserId): EitherT[F, AppError, Unit]
  def findById(id: UserId): EitherT[F, AppInternalError, Option[User]]
  def findByName(name: UserName): EitherT[F, AppInternalError, Option[User]]
  def findByEmail(email: UserEmail): EitherT[F, AppInternalError, Option[User]]
  def listAll(): EitherT[F, AppInternalError, List[UserTemplate]]

  def assignRoleToUser(userId: UserId, role: Role): EitherT[F, AppError, Unit]
  def removeRoleFromUser(userId: UserId, role: Role): EitherT[F, AppError, Unit]

}

object UserService {

  def make[F[_]: MonadCancelThrow](
    userRepo: UserRepository,
    roleRepo: RoleRepository,
    transactor: Transactor[F]
  ): UserService[F] = new UserService[F] {

    override def create(createUser: CreateUser): EitherT[F, AppError, User] =
      attemptEitherTTransaction(
        userRepo.create(createUser),
        transactor
      )

    override def update(userId: UserId, newUserInfo: UserInfo): EitherT[F, AppError, User] =
      attemptEitherTTransaction(
        userRepo.update(userId, newUserInfo),
        transactor
      )

    override def removeById(id: UserId): EitherT[F, AppError, Unit] =
      attemptEitherTTransaction(
        userRepo.removeById(id).leftMap(identity[AppError]),
        transactor
      )

    override def findById(id: UserId): EitherT[F, AppInternalError, Option[User]] =
      attemptTransaction(
        userRepo.findById(id),
        transactor
      )

    override def findByName(name: UserName): EitherT[F, AppInternalError, Option[User]] =
      attemptTransaction(
        userRepo.findByName(name),
        transactor
      )

    override def findByEmail(email: UserEmail): EitherT[F, AppInternalError, Option[User]] =
      attemptTransaction(
        userRepo.findByEmail(email),
        transactor
      )

    override def listAll(): EitherT[F, AppInternalError, List[UserTemplate]] =
      attemptTransaction(
        userRepo.listAll(),
        transactor
      )

    override def assignRoleToUser(userId: UserId, role: Role): EitherT[F, AppError, Unit] =
      attemptEitherTTransaction(
        roleRepo.assignRoleToUser(userId, role).leftMap(identity[AppError]),
        transactor
      )

    override def removeRoleFromUser(userId: UserId, role: Role): EitherT[F, AppError, Unit] =
      attemptEitherTTransaction(
        roleRepo.removeRoleFromUser(userId, role).leftMap(identity[AppError]),
        transactor
      )
  }

}
