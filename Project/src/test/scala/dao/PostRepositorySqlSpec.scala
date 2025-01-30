package dao

import domain.user._
import domain.post._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

import java.time.Instant

class PostRepositorySqlSpec extends AnyFunSuite with Matchers with IOChecker {

  val transactor = SharedPostgresContainer.transactor

  val post = Post(
    PostId(337),
    UserId(42),
    PostInfo(PostTitle("title"), PostContent("content")),
    PostCreationTime(Instant.now())
  )

  test("createSql") {
    check(
      PostRepository.sql.createSql(
        CreatePost(post.userId, post.postInfo)
      )
    )
  }

  test("updateSql") {
    check(
      PostRepository.sql.updateSql(
        post.id,
        post.postInfo
      )
    )
  }

  test("removeByIdSql") {
    check(
      PostRepository.sql.removeByIdSql(
        post.id
      )
    )
  }

  test("findByIdSql") {
    check(
      PostRepository.sql.findByIdSql(
        post.id
      )
    )
  }

  test("findByTitleSql") {
    check(
      PostRepository.sql.findByTitleSql(
        post.postInfo.title
      )
    )
  }

  test("listByUserIdSql") {
    check(
      PostRepository.sql.listByUserIdSql(
        post.userId
      )
    )
  }

  test("listAllSql") {
    check(
      PostRepository.sql.listAllSql
    )
  }

}
