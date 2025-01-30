import cats.data.EitherT
import cats.effect.MonadCancelThrow
import cats.implicits.{catsSyntaxEitherId, toFunctorOps}
import domain.errors.{AppError, AppInternalError}
import doobie._
import doobie.implicits._

package object service {

  def attemptEitherTTransaction[F[_]: MonadCancelThrow, R](
    transaction: EitherT[ConnectionIO, AppError, R],
    transactor: Transactor[F]
  ): EitherT[F, AppError, R] = EitherT(
    transaction.value
      .transact(transactor)
      .attemptSql
      .map {
        case Left(e)         => AppInternalError(e).asLeft
        case Right(Left(e))  => e.asLeft
        case Right(Right(v)) => v.asRight
      }
  )

  def attemptTransaction[F[_]: MonadCancelThrow, R](
    transaction: ConnectionIO[R],
    transactor: Transactor[F]
  ): EitherT[F, AppInternalError, R] = EitherT(
    transaction
      .transact(transactor)
      .attemptSql
  ).leftMap(AppInternalError(_))

}
