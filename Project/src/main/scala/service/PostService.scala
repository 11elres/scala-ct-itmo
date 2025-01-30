package service

import cats.data.EitherT
import cats.effect.MonadCancelThrow
import dao.PostRepository
import domain.errors.{AppError, AppInternalError}
import domain.post.{CreatePost, Post, PostId, PostInfo, PostTitle}
import domain.user.UserId
import doobie.util.transactor.Transactor

trait PostService[F[_]] {

  def create(post: CreatePost): EitherT[F, AppError, Post] // user not exist
  def updatePost(id: PostId, newPostInfo: PostInfo): EitherT[F, AppError, Post]
  def removeById(id: PostId): EitherT[F, AppError, Unit] // post not exist
  def findById(id: PostId): EitherT[F, AppInternalError, Option[Post]]
  def findByTitle(title: PostTitle): EitherT[F, AppInternalError, List[Post]]
  def listByUserId(id: UserId): EitherT[F, AppInternalError, List[Post]]
  def listAll(): EitherT[F, AppInternalError, List[Post]]

}

object PostService {

  def make[F[_]: MonadCancelThrow](
    postRepo: PostRepository,
    transactor: Transactor[F]
  ): PostService[F] = new PostService[F] {
    override def create(post: CreatePost): EitherT[F, AppError, Post] =
      attemptEitherTTransaction(
        postRepo.create(post).leftMap(identity[AppError]),
        transactor
      )

    override def updatePost(id: PostId, newPostInfo: PostInfo): EitherT[F, AppError, Post] =
      attemptEitherTTransaction(
        postRepo.updatePost(id, newPostInfo),
        transactor
      )

    override def removeById(id: PostId): EitherT[F, AppError, Unit] =
      attemptEitherTTransaction(
        postRepo.removeById(id).leftMap(identity[AppError]),
        transactor
      )

    override def findById(id: PostId): EitherT[F, AppInternalError, Option[Post]] =
      attemptTransaction(
        postRepo.findById(id),
        transactor
      )

    override def findByTitle(title: PostTitle): EitherT[F, AppInternalError, List[Post]] =
      attemptTransaction(
        postRepo.findByTitle(title),
        transactor
      )

    override def listByUserId(id: UserId): EitherT[F, AppInternalError, List[Post]] =
      attemptTransaction(
        postRepo.listByUserId(id),
        transactor
      )

    override def listAll(): EitherT[F, AppInternalError, List[Post]] =
      attemptTransaction(
        postRepo.listAll(),
        transactor
      )
  }

}
