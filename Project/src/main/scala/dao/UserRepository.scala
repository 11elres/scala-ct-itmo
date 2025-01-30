package dao

import cats.data.OptionT.liftF
import cats.data._
import cats.free.Free
import cats.syntax.either._
import domain.errors._
import domain.role.OrdinaryUserRole
import domain.user._
import doobie._
import doobie.implicits._

trait UserRepository {

  def create(user: CreateUser): EitherT[ConnectionIO, AppError, User]
  def update(id: UserId, newUserInfo: UserInfo): EitherT[ConnectionIO, AppError, User]
  def removeById(id: UserId): EitherT[ConnectionIO, UserNotFoundError, Unit]
  def findById(id: UserId): ConnectionIO[Option[User]]
  def findByName(name: UserName): ConnectionIO[Option[User]]
  def findByEmail(email: UserEmail): ConnectionIO[Option[User]]
  def listAll(): ConnectionIO[List[UserTemplate]]

}

object UserRepository {

  object sql {

    def createSql(user: CreateUser): Update0 =
      sql"""
           INSERT INTO users (username, email, status, about, password_hash)
           VALUES
            (${user.userInfo.name.value},
             ${user.userInfo.email.value},
             ${user.userInfo.status.value},
             ${user.userInfo.about.value},
             ${user.passwordHash.value})
        """.update

    def updateSql(id: UserId, newUserInfo: UserInfo): Update0 =
      sql"""
           UPDATE users
           SET username = ${newUserInfo.name.value},
               email = ${newUserInfo.email.value},
               status = ${newUserInfo.status.value},
               about = ${newUserInfo.about.value}
           WHERE id = ${id.value}
         """.update

    def removeByIdSql(id: UserId): Update0 =
      sql"""
           DELETE FROM users
           WHERE id=${id.value}
         """.update

    def findByIdSql(id: UserId): Query0[UserTemplate] =
      sql"""
           SELECT id, username, email, status, about, password_hash, created_at
           FROM users
           WHERE id=${id.value}
         """.query[UserTemplate]

    def findByNameSql(name: UserName): Query0[UserTemplate] =
      sql"""
           SELECT id, username, email, status, about, password_hash, created_at
           FROM users
           WHERE username=${name.value}
         """.query[UserTemplate]

    def findByEmailSql(email: UserEmail): Query0[UserTemplate] =
      sql"""
           SELECT id, username, email, status, about, password_hash, created_at
           FROM users
           WHERE email=${email.value}
         """.query[UserTemplate]

    val listAllSql: Query0[UserTemplate] =
      sql"""
           SELECT id, username, email, status, about, password_hash, created_at
           FROM users
         """.query[UserTemplate]

  }

  def make(roleRepo: RoleRepository): UserRepository = new UserRepository {

    private def checkEmail(
      email: UserEmail
    ): EitherT[ConnectionIO, UserEmailIsAlreadyOccupiedError, Unit] =
      EitherT(
        findByEmail(email)
          .map {
            case Some(_) => UserEmailIsAlreadyOccupiedError(email).asLeft
            case None    => ().asRight[UserEmailIsAlreadyOccupiedError]
          }
      )

    private def checkName(
      name: UserName
    ): EitherT[ConnectionIO, UserNameIsAlreadyOccupiedError, Unit] =
      EitherT(
        findByName(name)
          .map {
            case Some(_) => UserNameIsAlreadyOccupiedError(name).asLeft
            case None    => ().asRight[UserNameIsAlreadyOccupiedError]
          }
      )

    override def create(
      user: CreateUser
    ): EitherT[ConnectionIO, AppError, User] =
      for {
        // correctness
        _ <- checkName(user.userInfo.name)
        _ <- checkEmail(user.userInfo.email)
        // creating
        userId <- EitherT(
          sql
            .createSql(user)
            .withUniqueGeneratedKeys[UserId]("id")
            .map(_.asRight[AppError])
        )
        _ <- roleRepo.assignRoleToUser(userId, OrdinaryUserRole)
        user <- EitherT(
          findById(userId).map(
            _.toRight(
              AppInternalError(
                new IllegalStateException(
                  s"Couldn't find user with ${userId.value} id after creation."
                )
              )
            )
          )
        ).leftMap(identity[AppError])
      } yield user

    override def update(
      id: UserId,
      newUserInfo: UserInfo
    ): EitherT[ConnectionIO, AppError, User] =
      for {
        user <- EitherT(findById(id).map(_.toRight(UserNotFoundError(id))))

        // correctness
        _ <-
          if (user.userInfo.name == newUserInfo.name)
            EitherT.pure[ConnectionIO, AppError](())
          else
            checkName(newUserInfo.name)

        _ <-
          if (user.userInfo.email == newUserInfo.email)
            EitherT.pure[ConnectionIO, AppError](())
          else
            checkEmail(newUserInfo.email)

        // updating
        newUser <- EitherT[ConnectionIO, AppError, User](
          sql.updateSql(id, newUserInfo).run.flatMap {
            case 0 => Free.pure(UserNotFoundError(id).asLeft)
            case _ =>
              findById(id).map(
                _.toRight(
                  AppInternalError(
                    new IllegalStateException(
                      s"Couldn't find user with ${id.value} id after updating."
                    )
                  )
                )
              )
          }
        )
      } yield newUser

    override def removeById(
      id: UserId
    ): EitherT[ConnectionIO, UserNotFoundError, Unit] = EitherT(sql.removeByIdSql(id).run.map {
      case 0 => UserNotFoundError(id).asLeft
      case _ => ().asRight
    })

    private def addRoles(
      connIOUserTemplate: ConnectionIO[Option[UserTemplate]]
    ): ConnectionIO[Option[User]] =
      (for {
        template <- OptionT(connIOUserTemplate)
        roles    <- liftF(roleRepo.getUserRoles(template.id))
      } yield template.toUser(roles)).value

    override def findById(
      id: UserId
    ): ConnectionIO[Option[User]] = addRoles(sql.findByIdSql(id).option)

    override def findByName(
      name: UserName
    ): ConnectionIO[Option[User]] = addRoles(sql.findByNameSql(name).option)

    override def findByEmail(
      email: UserEmail
    ): ConnectionIO[Option[User]] = addRoles(sql.findByEmailSql(email).option)

    override def listAll(): ConnectionIO[List[UserTemplate]] = sql.listAllSql.to[List]
  }

}
