package dao

import cats.data._
import cats.free.Free
import cats.syntax.either._
import domain.errors._
import domain.post._
import domain.user.UserId
import doobie._
import doobie.implicits._

trait PostRepository {

  def create(post: CreatePost): EitherT[ConnectionIO, AppInternalError, Post] // user not exist
  def updatePost(id: PostId, newPostInfo: PostInfo): EitherT[ConnectionIO, AppError, Post]
  def removeById(id: PostId): EitherT[ConnectionIO, PostNotFoundError, Unit] // post not exist
  def findById(id: PostId): ConnectionIO[Option[Post]]
  def findByTitle(title: PostTitle): ConnectionIO[List[Post]]
  def listByUserId(id: UserId): ConnectionIO[List[Post]]
  def listAll(): ConnectionIO[List[Post]]

}

object PostRepository {

  object sql {

    def createSql(post: CreatePost): Update0 =
      sql"""
           INSERT INTO posts (user_id, title, content)
           VALUES (${post.user.value}, ${post.postInfo.title.value}, ${post.postInfo.content.value})
        """.update

    def updateSql(id: PostId, newPostInfo: PostInfo): Update0 =
      sql"""
           UPDATE posts
           SET title = ${newPostInfo.title.value},
               content = ${newPostInfo.content.value}
           WHERE id = ${id.value}
         """.update

    def removeByIdSql(id: PostId): Update0 =
      sql"""
           DELETE FROM posts
           WHERE id=${id.value}
         """.update

    def findByIdSql(id: PostId): Query0[Post] =
      sql"""
           SELECT id, user_id, title, content, created_at
           FROM posts
           WHERE id=${id.value}
         """.query[Post]

    def findByTitleSql(title: PostTitle): Query0[Post] =
      sql"""
           SELECT id, user_id, title, content, created_at
           FROM posts
           WHERE title=${title.value}
         """.query[Post]

    def listByUserIdSql(id: UserId): Query0[Post] =
      sql"""
           SELECT id, user_id, title, content, created_at
           FROM posts
           WHERE user_id=${id.value}
         """.query[Post]

    val listAllSql: Query0[Post] =
      sql"""
           SELECT id, user_id, title, content, created_at
           FROM posts
         """.query[Post]

  }

  def make(): PostRepository = new PostRepository {

    override def create(post: CreatePost): EitherT[ConnectionIO, AppInternalError, Post] =
      for {
        postId <- EitherT(
          sql
            .createSql(post)
            .withUniqueGeneratedKeys[PostId]("id")
            .map(_.asRight[AppInternalError])
        )
        post <- EitherT(
          findById(postId).map(
            _.toRight(
              AppInternalError(
                new IllegalStateException("Couldn't find post after creation.")
              )
            )
          )
        )
      } yield post

    override def updatePost(
      id: PostId,
      newPostInfo: PostInfo
    ): EitherT[ConnectionIO, AppError, Post] = EitherT(
      sql.updateSql(id, newPostInfo).run.flatMap {
        case 0 => Free.pure(PostNotFoundError(id).asLeft)
        case _ =>
          findById(id).map(
            _.toRight(
              AppInternalError(
                new IllegalStateException("Couldn't find post after updating.")
              )
            )
          )
      }
    )

    override def removeById(id: PostId): EitherT[ConnectionIO, PostNotFoundError, Unit] = EitherT(
      sql.removeByIdSql(id).run.map {
        case 0 => PostNotFoundError(id).asLeft
        case _ => ().asRight
      }
    )

    override def findById(id: PostId): ConnectionIO[Option[Post]] = sql.findByIdSql(id).option

    override def findByTitle(title: PostTitle): ConnectionIO[List[Post]] =
      sql.findByTitleSql(title).to[List]

    override def listByUserId(userId: UserId): ConnectionIO[List[Post]] =
      sql.listByUserIdSql(userId).to[List]

    override def listAll(): ConnectionIO[List[Post]] = sql.listAllSql.to[List]
  }

}
