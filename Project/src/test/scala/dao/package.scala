import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import cats.implicits._
import doobie._
import doobie.implicits._

import scala.io.Source

package object dao {

  def initDB(transactor: Transactor[IO]): Unit = {
    val initFile = Resource.fromAutoCloseable(IO(Source.fromFile("init.sql")))
    val queries = initFile
      .use(source => IO(source.mkString))
      .unsafeRunSync()
      .split(";")
      .map(_.trim)
      .filter(_.nonEmpty)
      .toList
    queries
      .foldLeft(().pure[ConnectionIO]) { (acc, query) =>
        acc.flatMap(_ => Fragment.const(query).update.run.void)
      }
      .transact(transactor)
      .unsafeRunSync()
  }

}
