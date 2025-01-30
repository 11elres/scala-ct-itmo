package dao

import org.scalatest.{Args, Status, Suites}

class SqlSpec
    extends Suites(
      new UserRepositorySqlSpec,
      new RoleRepositorySqlSpec,
      new PostRepositorySqlSpec
    ) {

  override def run(testName: Option[String], args: Args): Status = {
    initDB(SharedPostgresContainer.transactor)
    try
      super.run(testName, args)
    finally
      SharedPostgresContainer.container.stop()
  }

}
