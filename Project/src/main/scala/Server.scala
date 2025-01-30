import cats.effect.kernel.Resource
import cats.effect._
import com.comcast.ip4s._
import com.zaxxer.hikari.HikariConfig
import config.AppConfig
import controller._
import dao._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import service._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt
import validation._

object Server extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      authSecretKey <- HMACSHA256.generateKey[IO]

      exitCode <- (for {
        config <- Resource.eval(AppConfig.load)

        hikariConfig <- Resource.eval(IO {
          val hikariConfig = new HikariConfig()
          hikariConfig.setJdbcUrl(config.db.url)
          hikariConfig.setUsername(config.db.user)
          hikariConfig.setPassword(config.db.password)
          hikariConfig.setMaximumPoolSize(10)
          hikariConfig
        })

        transactor <- for {
          ec <- ExecutionContexts.fixedThreadPool[IO](10)
          xa <- HikariTransactor.fromHikariConfig[IO](hikariConfig, ec)
        } yield xa

        // repos
        roleRepo <- Resource.eval(RoleRepository.make(transactor))
        userRepo = UserRepository.make(roleRepo)
        postRepo = PostRepository.make()
        // services
        tokenService = TokenService.make[IO, HMACSHA256](
          TokenService.JWTWrapper.make(authSecretKey)
        )
        userService = UserService.make[IO](userRepo, roleRepo, transactor)
        authService = AuthService.make[IO, BCrypt](userService, UserValidator.make())
        postService = PostService.make[IO](postRepo, transactor)
        // controllers
        userController = UserController.make(userService, tokenService)
        authController = AuthController.make(authService, tokenService)
        postController = PostController.make(postService, tokenService, userService)

        serverEndpoints = userController.all ++ authController.all ++ postController.all
        docsEndpoints = SwaggerInterpreter().fromEndpoints[IO](
          endpoints.all,
          "SimpleNetwork API",
          "1.0"
        )
        routes = Http4sServerInterpreter[IO]().toRoutes(
          serverEndpoints ++ docsEndpoints
        )
        httpApp = Router("/" -> routes).orNotFound
        _ <- EmberServerBuilder
          .default[IO]
          .withHost(
            Ipv4Address.fromString(config.server.host).getOrElse(ipv4"0.0.0.0")
          )
          .withPort(Port.fromInt(config.server.port).getOrElse(port"80"))
          .withHttpApp(httpApp)
          .build
      } yield ()).useForever.as(ExitCode.Success)
    } yield exitCode
  }

}
