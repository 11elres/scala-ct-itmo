package task1.hierarchy

import scala.util.control.TailCalls.{done, TailRec}
import task1.{Branch, Leaf, Tree}

trait Functor[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
}

trait Apply[F[_]] extends Functor[F] {
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]
}

trait Applicative[F[_]] extends Apply[F] {
  def pure[A](a: A): F[A]
}

trait FlatMap[F[_]] extends Apply[F] {
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  /** Несмотря на название, в этом задании необязательно реализовывать через @tailrec. Но обязательно, чтоб он был
    * стекобезопасным.
    */
  def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B]
}

trait Monad[F[_]] extends FlatMap[F] with Applicative[F] {
  def pure[A](a: A): F[A]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
}

object TypeClasses {

  implicit val treeMonad: Monad[Tree] = new Monad[Tree] {
    override def map[A, B](fa: Tree[A])(f: A => B): Tree[B] = flatMap(fa)(a => Leaf(f(a)))

    override def ap[A, B](ff: Tree[A => B])(fa: Tree[A]): Tree[B] =
      flatMap(ff)(f => flatMap(fa)(a => pure(f(a))))

    def pure[A](a: A): Tree[A] = Leaf(a)

    private def flatMapHelp[A, B](fa: Tree[A])(f: A => TailRec[Tree[B]]): TailRec[Tree[B]] =
      fa match {
        case Leaf(value) => done(value).flatMap(f)
        case Branch(left, right) =>
          for {
            l <- flatMapHelp(left)(f)
            r <- flatMapHelp(right)(f)
          } yield Branch(l, r)
      }

    def flatMap[A, B](fa: Tree[A])(f: A => Tree[B]): Tree[B] = flatMapHelp(fa)(done(_).map(f)).result

    override def tailRecM[A, B](a: A)(f: A => Tree[Either[A, B]]): Tree[B] =
      flatMapHelp(f(a)) {
        lazy val function: Either[A, B] => TailRec[Tree[B]] = {
          case Right(value) => done(Leaf(value))
          case Left(value)  => done(f(value)).flatMap(flatMapHelp(_)(function))
        }
        function
      }.result
  }
}
