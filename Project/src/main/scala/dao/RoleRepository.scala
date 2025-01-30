package dao

import cats.data.EitherT
import cats.data.EitherT.liftF
import cats.effect.MonadCancelThrow
import cats.syntax.all._
import domain.errors._
import domain.role._
import domain.user._
import doobie._
import doobie.implicits._

trait RoleRepository {

  def checkUserRole(userId: UserId, role: Role): ConnectionIO[Boolean]

  def assignRoleToUser(
    userId: UserId,
    role: Role
  ): EitherT[ConnectionIO, RoleAlreadyAssignedError, Unit]

  def removeRoleFromUser(
    userId: UserId,
    role: Role
  ): EitherT[ConnectionIO, RoleNotAssignedError, Unit]

  def getUserRoles(userId: UserId): ConnectionIO[Set[Role]]

}

object RoleRepository {

  object sql {

    val getRolesMappingSql: Query0[(RoleId, String)] =
      sql"""
           SELECT id, name
           FROM roles
         """.query[(RoleId, String)]

    def checkUserRoleSql(userId: UserId, roleId: RoleId): Query0[Long] =
      sql"""
           SELECT COUNT(*)
           FROM user_roles
           WHERE user_id=${userId.value} AND role_id=${roleId.value}
         """.query[Long]

    def assignRoleToUserSql(userId: UserId, roleId: RoleId): Update0 =
      sql"""
           INSERT INTO user_roles (user_id, role_id)
           VALUES (${userId.value}, ${roleId.value})
         """.update

    def removeRoleFromUserSql(userId: UserId, roleId: RoleId): Update0 =
      sql"""
           DELETE FROM user_roles
           WHERE user_id=${userId.value}
             AND role_id=${roleId.value}
         """.update

    def getUserRoles(userId: UserId): Query0[RoleId] =
      sql"""
           SELECT role_id
           FROM user_roles
           WHERE user_id=${userId.value}
         """.query[RoleId]

  }

  def make[F[_]: MonadCancelThrow](
    transactor: Transactor[F]
  ): F[RoleRepository] =
    for {
      // get roles from db
      mapping0 <- sql.getRolesMappingSql.to[List].transact(transactor)
      mapping = mapping0.flatMap(pair => Role.toRole(pair._2).map((pair._1, _)))

      // checking and mapping
      rolesInDB = mapping.map(_._2).toSet
      _ <-
        if (List(OrdinaryUserRole, CensorRole, AdminRole).forall(rolesInDB.contains))
          MonadCancelThrow[F].pure(())
        else
          MonadCancelThrow[F].raiseError(
            new IllegalStateException("Database should contains all roles.")
          )
      idToRole = mapping.toMap
      roleToId = mapping.map(pair => (pair._2, pair._1)).toMap

      repo = new RoleRepository {

        override def checkUserRole(userId: UserId, role: Role): ConnectionIO[Boolean] =
          sql.checkUserRoleSql(userId, roleToId(role)).unique.map(_ > 0)

        override def assignRoleToUser(
          userId: UserId,
          role: Role
        ): EitherT[ConnectionIO, RoleAlreadyAssignedError, Unit] =
          for {
            existing <- liftF(checkUserRole(userId, role))
            _ <-
              if (existing)
                EitherT
                  .leftT[ConnectionIO, Unit](RoleAlreadyAssignedError(userId, role))
              else
                liftF[ConnectionIO, RoleAlreadyAssignedError, Unit](
                  sql.assignRoleToUserSql(userId, roleToId(role)).run.map(_ => ())
                )
          } yield ()

        override def removeRoleFromUser(
          userId: UserId,
          role: Role
        ): EitherT[ConnectionIO, RoleNotAssignedError, Unit] =
          for {
            existing <- liftF(checkUserRole(userId, role))
            _ <-
              if (existing)
                liftF[ConnectionIO, RoleNotAssignedError, Unit](
                  sql.removeRoleFromUserSql(userId, roleToId(role)).run.map(_ => ())
                )
              else
                EitherT
                  .leftT[ConnectionIO, Unit](RoleNotAssignedError(userId, role))
          } yield ()

        override def getUserRoles(userId: UserId): ConnectionIO[Set[Role]] =
          sql.getUserRoles(userId).to[Set].map(_.map(idToRole(_)))
      }
    } yield repo

}
