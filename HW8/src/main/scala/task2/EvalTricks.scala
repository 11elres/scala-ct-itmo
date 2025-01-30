package task2

import cats.Eval

object EvalTricks {

  def fib(n: Int): Eval[BigInt] = {
    def fibHelp(acc1: BigInt, acc2: BigInt, n: Int): Eval[BigInt] = {
      if (n == 0)
        Eval.now(acc1)
      else
        Eval.defer(fibHelp(acc2, acc1 + acc2, n - 1))
    }
    fibHelp(0, 1, n)
  }

  def foldRight[A, B](as: List[A], acc: Eval[B])(fn: (A, Eval[B]) => Eval[B]): Eval[B] = {
    if (as.isEmpty)
      acc
    else
      Eval.defer(fn(as.head, foldRight(as.tail, acc)(fn)))
  }

}
