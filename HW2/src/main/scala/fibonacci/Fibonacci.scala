package fibonacci

import scala.annotation.tailrec

object Fibonacci {

  def fibonacci(limit: Long): BigInt = {
    @tailrec
    def fibHelp(n: Long, acc1: BigInt, acc2: BigInt): BigInt =
      if n == 0 then acc1 else fibHelp(n - 1, acc2, acc1 + acc2)
    fibHelp(limit, 0, 1)
  }

}
