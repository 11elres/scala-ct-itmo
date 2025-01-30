package dao

import domain.role.OrdinaryUserRole
import domain.user._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

import java.time.Instant

class UserRepositorySqlSpec extends AnyFunSuite with Matchers with IOChecker {

  val transactor = SharedPostgresContainer.transactor

  val user = User(
    UserId(42),
    UserInfo(UserName("name"), UserEmail("email@email.com"), UserStatus(""), UserAbout("")),
    Set(OrdinaryUserRole),
    UserPasswordHash("$widfhhco$uiadhcvkdsvkceiufhliwehfu"),
    UserCreationTime(Instant.now())
  )

  test("createSql") {
    check(
      UserRepository.sql.createSql(
        CreateUser(user.userInfo, user.passwordHash)
      )
    )
  }

  test("updateSql") {
    check(
      UserRepository.sql.updateSql(
        user.id,
        user.userInfo
      )
    )
  }

  test("removeByIdSql") {
    check(
      UserRepository.sql.removeByIdSql(
        user.id
      )
    )
  }

  test("findByIdSql") {
    check(
      UserRepository.sql.findByIdSql(
        user.id
      )
    )
  }

  test("findByNameSql") {
    check(
      UserRepository.sql.findByNameSql(
        user.userInfo.name
      )
    )
  }

  test("findByEmailSql") {
    check(
      UserRepository.sql.findByEmailSql(
        user.userInfo.email
      )
    )
  }

  test("listSql") {
    check(
      UserRepository.sql.listAllSql
    )
  }

}
