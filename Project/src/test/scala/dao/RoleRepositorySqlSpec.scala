package dao

import domain.role.RoleId
import domain.user._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

class RoleRepositorySqlSpec extends AnyFunSuite with Matchers with IOChecker {

  val transactor = SharedPostgresContainer.transactor

  val userId = UserId(42)
  val roleId = RoleId(2)

  test("getRoleMappingSql") {
    check(
      RoleRepository.sql.getRolesMappingSql
    )
  }

  test("checkUserRoleSql") {
    check(
      RoleRepository.sql.checkUserRoleSql(userId, roleId)
    )
  }

  test("assignRoleToUserSql") {
    check(
      RoleRepository.sql.assignRoleToUserSql(userId, roleId)
    )
  }

  test("removeRoleFromUserSql") {
    check(
      RoleRepository.sql.removeRoleFromUserSql(userId, roleId)
    )
  }

  test("getUserRoles") {
    check(
      RoleRepository.sql.getUserRoles(userId)
    )
  }

}
