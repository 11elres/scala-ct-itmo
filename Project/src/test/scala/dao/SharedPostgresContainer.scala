package dao

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import org.testcontainers.utility.DockerImageName

object SharedPostgresContainer {

  lazy val container = PostgreSQLContainer
    .Def(
      dockerImageName = DockerImageName.parse("postgres:13"),
      databaseName = "testcontainer-scala",
      username = "scala",
      password = "scala"
    )
    .start()

  lazy val transactor = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = SharedPostgresContainer.container.jdbcUrl,
    user = SharedPostgresContainer.container.username,
    pass = SharedPostgresContainer.container.password
  )

}
