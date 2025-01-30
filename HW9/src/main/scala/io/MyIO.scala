package io

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
import scala.util.control.TailCalls.{TailRec, done, tailcall}

/** Класс типов, позволяющий комбинировать описания вычислений, которые могут либо успешно
  * завершиться с некоторым значением, либо завершиться неуспешно, выбросив исключение Throwable.
  * @tparam F
  *   тип вычисления
  */
trait Computation[F[_]] {

  def map[A, B](fa: F[A])(f: A => B): F[B]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B]
  def pure[A](a: A): F[A]
  def *>[A, B](fa: F[A])(another: F[B]): F[B]
  def as[A, B](fa: F[A])(newValue: => B): F[B]
  def void[A](fa: F[A]): F[Unit]
  def attempt[A](fa: F[A]): F[Either[Throwable, A]]
  def option[A](fa: F[A]): F[Option[A]]

  /** Если вычисление fa выбрасывает ошибку, то обрабатывает ее функцией f, без изменения типа
    * выходного значения.
    * @return
    *   результат вычисления fa или результат функции f
    */
  def handleErrorWith[A, AA >: A](fa: F[A])(f: Throwable => F[AA]): F[AA]

  /** Обрабатывает ошибку вычисления чистой функцией recover или преобразует результат вычисления
    * чистой функцией.
    * @return
    *   результат вычисления преобразованный функцией map или результат функции recover
    */
  def redeem[A, B](fa: F[A])(recover: Throwable => B, map: A => B): F[B]
  def redeemWith[A, B](fa: F[A])(recover: Throwable => F[B], bind: A => F[B]): F[B]

  /** Выполняет вычисление. "unsafe", потому что при неуспешном завершении может выбросить
    * исключение.
    * @param fa
    *   еще не начавшееся вычисление
    * @tparam A
    *   тип результата вычисления
    * @return
    *   результат вычисления, если оно завершится успешно.
    */
  def unsafeRunSync[A](fa: F[A]): A

  /** Оборачивает ошибку в контекст вычисления.
    * @param error
    *   ошибка
    * @tparam A
    *   тип результата вычисления. Т.к. вычисление сразу завершится ошибкой при выполнении, то может
    *   быть любым.
    * @return
    *   создает описание вычисления, которое сразу же завершается с поданной ошибкой.
    */
  def raiseError[A](error: Throwable): F[A]

}

object Computation {
  def apply[F[_]: Computation]: Computation[F] = implicitly[Computation[F]]
}

final class MyIO[A](private val content: TailRec[Try[A]]) {
  self =>

  def map[B](f: A => B)(implicit
    comp: Computation[MyIO]
  ): MyIO[B] = comp.map(self)(f)

  def flatMap[B](f: A => MyIO[B])(implicit
    comp: Computation[MyIO]
  ): MyIO[B] = comp.flatMap(self)(f)

  def tailRecM[B](f: A => MyIO[Either[A, B]])(implicit
    comp: Computation[MyIO]
  ): MyIO[B] = comp.tailRecM(comp.unsafeRunSync(self))(f)

  def *>[B](another: MyIO[B])(implicit
    comp: Computation[MyIO]
  ): MyIO[B] = comp.*>(self)(another)

  def as[B](newValue: => B)(implicit
    comp: Computation[MyIO]
  ): MyIO[B] = comp.as(self)(newValue)

  def void(implicit
    comp: Computation[MyIO]
  ): MyIO[Unit] = comp.void(self)

  def attempt(implicit
    comp: Computation[MyIO]
  ): MyIO[Either[Throwable, A]] = comp.attempt(self)

  def option(implicit
    comp: Computation[MyIO]
  ): MyIO[Option[A]] = comp.option(self)

  def handleErrorWith[AA >: A](f: Throwable => MyIO[AA])(implicit
    comp: Computation[MyIO]
  ): MyIO[AA] = comp.handleErrorWith[A, AA](self)(f)

  def redeem[B](recover: Throwable => B, map: A => B)(implicit
    comp: Computation[MyIO]
  ): MyIO[B] = comp.redeem(self)(recover, map)

  def redeemWith[B](recover: Throwable => MyIO[B], bind: A => MyIO[B])(implicit
    comp: Computation[MyIO]
  ): MyIO[B] = comp.redeemWith(self)(recover, bind)

  def unsafeRunSync(implicit
    comp: Computation[MyIO]
  ): A = comp.unsafeRunSync(self)

}

object MyIO {

  implicit val computationInstanceForIO: Computation[MyIO] = new Computation[MyIO] {
    override def map[A, B](fa: MyIO[A])(f: A => B): MyIO[B] = flatMap(fa)(a => MyIO(f(a)))
    override def flatMap[A, B](fa: MyIO[A])(f: A => MyIO[B]): MyIO[B] =
      redeemWith(fa)(raiseError, f)
    override def tailRecM[A, B](a: A)(f: A => MyIO[Either[A, B]]): MyIO[B] = {
      @tailrec
      def loop(io: MyIO[Either[A, B]]): B = unsafeRunSync(io) match {
        case Left(value)  => loop(f(value))
        case Right(value) => value
      }
      MyIO(loop(f(a)))
    }
    override def pure[A](a: A): MyIO[A]                              = MyIO(a)
    override def *>[A, B](fa: MyIO[A])(another: MyIO[B]): MyIO[B]    = flatMap(fa)(_ => another)
    override def as[A, B](fa: MyIO[A])(newValue: => B): MyIO[B]      = *>(fa)(MyIO(newValue))
    override def void[A](fa: MyIO[A]): MyIO[Unit]                    = flatMap(fa)(_ => unit)
    override def attempt[A](fa: MyIO[A]): MyIO[Either[Throwable, A]] = redeem(fa)(Left(_), Right(_))
    override def option[A](fa: MyIO[A]): MyIO[Option[A]] = redeem(fa)(_ => None, Some(_))
    override def handleErrorWith[A, AA >: A](fa: MyIO[A])(f: Throwable => MyIO[AA]): MyIO[AA] =
      redeemWith(fa)(f, MyIO(_))
    override def redeem[A, B](fa: MyIO[A])(recover: Throwable => B, map: A => B): MyIO[B] =
      redeemWith(fa)(t => MyIO(recover(t)), a => MyIO(map(a)))
    override def redeemWith[A, B](
      fa: MyIO[A]
    )(recover: Throwable => MyIO[B], bind: A => MyIO[B]): MyIO[B] = new MyIO(
      tailcall {
        fa.content.flatMap {
          case Failure(exception) => recover(exception).content
          case Success(value)     => bind(value).content
        }
      }
    )
    override def unsafeRunSync[A](fa: MyIO[A]): A         = fa.content.result.get
    override def raiseError[A](error: Throwable): MyIO[A] = MyIO(throw error)
  }

  def apply[A](body: => A): MyIO[A] = new MyIO(tailcall(done(Try(body))))

  def suspend[A](thunk: => MyIO[A]): MyIO[A] = new MyIO(tailcall(thunk.content))

  def delay[A](body: => A): MyIO[A] = MyIO(body)
  def pure[A](a: A): MyIO[A]        = MyIO(a)

  def fromEither[A](e: Either[Throwable, A]): MyIO[A] = MyIO(e.fold(throw _, identity))

  def fromOption[A](option: Option[A])(orElse: => Throwable): MyIO[A] = MyIO(
    option.getOrElse(throw orElse)
  )

  def fromTry[A](t: Try[A]): MyIO[A] = MyIO(t.get)

  def none[A]: MyIO[Option[A]] = MyIO(None)

  def raiseUnless(cond: Boolean)(e: => Throwable)(implicit
    comp: Computation[MyIO]
  ): MyIO[Unit] = if (!cond) raiseError(e)(comp) else unit

  def raiseWhen(cond: Boolean)(e: => Throwable)(implicit
    comp: Computation[MyIO]
  ): MyIO[Unit] = if (cond) raiseError(e)(comp) else unit

  def raiseError[A](error: Throwable)(implicit
    comp: Computation[MyIO]
  ): MyIO[A] = comp.raiseError(error)

  def unlessA(cond: Boolean)(action: => MyIO[Unit]): MyIO[Unit] = if (!cond) action else unit
  def whenA(cond: Boolean)(action: => MyIO[Unit]): MyIO[Unit]   = if (cond) action else unit
  val unit: MyIO[Unit]                                          = MyIO(())

}
