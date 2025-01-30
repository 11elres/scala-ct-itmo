package controller

import cats.data.EitherT
import cats.effect.IO
import domain.authentication.UserClaims
import domain.errors._
import domain.post.{CreatePost, Post}
import domain.role.{CensorRole, OrdinaryUserRole}
import service._
import sttp.tapir.server.ServerEndpoint

trait PostController[F[_]] {

  def createPost: ServerEndpoint[Any, F]
  def postById: ServerEndpoint[Any, F]
  def postByTitle: ServerEndpoint[Any, F]
  def deletePost: ServerEndpoint[Any, F]
  def updatePost: ServerEndpoint[Any, F]
  def listUserPosts: ServerEndpoint[Any, F]
  def listPosts: ServerEndpoint[Any, F]
  def all: List[ServerEndpoint[Any, F]]

}

object PostController {

  def make(
    postService: PostService[IO],
    tokenService: TokenService[IO],
    userService: UserService[IO]
  ): PostController[IO] = new PostController[IO] {

    override def createPost: ServerEndpoint[Any, IO] =
      endpoints.createPost
        .serverSecurityLogic[UserClaims, IO](tokenService.verifyUser(_).value)
        .serverLogic(claims =>
          postInfo =>
            (if (claims.roles.contains(OrdinaryUserRole))
               postService.create(CreatePost(claims.id, postInfo))
             else
               EitherT
                 .leftT[IO, Post](
                   PermissionDeniedError(
                     s"Creating post: User with ${claims.id} id hasn't permissions to publish posts"
                   )
                 )
                 .leftMap(identity[AppError])).value
        )

    override def postById: ServerEndpoint[Any, IO] =
      endpoints.postById
        .serverLogic(id =>
          (for {
            opt <- postService.findById(id).leftMap(identity[AppError])
            post <- EitherT
              .fromEither[IO](opt.toRight(PostNotFoundError(id)))
              .leftMap(identity[AppError])
          } yield post).value
        )

    override def postByTitle: ServerEndpoint[Any, IO] =
      endpoints.postByTitle
        .serverLogic(title => postService.findByTitle(title).leftMap(identity[AppError]).value)

    override def deletePost: ServerEndpoint[Any, IO] =
      endpoints.deletePost
        .serverSecurityLogic[UserClaims, IO](tokenService.verifyUser(_).value)
        .serverLogic(claims =>
          id =>
            (for {
              opt  <- postService.findById(id).leftMap(identity[AppError])
              post <- EitherT.fromEither[IO](opt.toRight(PostNotFoundError(id)))
              _ <-
                if (claims.roles.contains(CensorRole) || post.userId == claims.id)
                  postService.removeById(id)
                else
                  EitherT
                    .leftT[IO, Unit](
                      PermissionDeniedError(
                        s"Deleting post: User with ${claims.id.value} tries to delete post with ${post.id.value} id"
                      )
                    )
                    .leftMap(identity[AppError])
            } yield ()).value
        )

    override def updatePost: ServerEndpoint[Any, IO] =
      endpoints.updatePost
        .serverSecurityLogic[UserClaims, IO](tokenService.verifyUser(_).value)
        .serverLogic(claims =>
          idAndInfo =>
            (for {
              userOpt <- userService.findById(claims.id)
              user <- EitherT
                .fromEither[IO](userOpt.toRight(UserNotFoundError(claims.id)))
                .leftMap(identity[AppError])

              updatedPost <-
                if (claims.id == user.id)
                  postService.updatePost(idAndInfo._1, idAndInfo._2)
                else
                  EitherT
                    .leftT[IO, Post](
                      PermissionDeniedError(
                        s"Updating post: Post with ${idAndInfo._1.value} id isn't belong to user with ${user.id.value} id"
                      )
                    )
                    .leftMap(identity[AppError])
            } yield updatedPost).value
        )

    override def listUserPosts: ServerEndpoint[Any, IO] =
      endpoints.listUserPosts
        .serverLogic(name =>
          (for {
            user <- userService
              .findByName(name)
              .flatMap(opt =>
                EitherT
                  .fromEither[IO](opt.toRight(UserByNameNotFoundError(name)))
                  .leftMap(identity[AppError])
              )
            posts <- postService.listByUserId(user.id).leftMap(identity[AppError])
          } yield posts).value
        )

    override def listPosts: ServerEndpoint[Any, IO] =
      endpoints.listPosts
        .serverLogic(_ => postService.listAll().leftMap(identity[AppError]).value)

    override def all: List[ServerEndpoint[Any, IO]] =
      List(createPost, postById, postByTitle, deletePost, updatePost, listUserPosts, listPosts)
  }

}
