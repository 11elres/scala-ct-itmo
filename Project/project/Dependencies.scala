import sbt.*

object Dependencies {

  val `cats-effect` = "org.typelevel"                 %% "cats-effect"         % "3.5.0"
  val client3       = "com.softwaremill.sttp.client3" %% "core"                % "3.8.15"
  val newtype       = "io.estatico"                   %% "newtype"             % "0.4.4"
  val pureconfig    = "com.github.pureconfig"         %% "pureconfig"          % "0.17.4"
  val ember         = "org.http4s"                    %% "http4s-ember-server" % "0.23.19"
  val scalatest     = "org.scalatest"                 %% "scalatest"           % "3.2.9"   % Test
  val mockito       = "org.mockito"                   %% "mockito-scala"       % "1.17.12" % Test
  val postgres      = "org.postgresql"                 % "postgresql"          % "42.5.1"  % Test

  object testcontainers {

    val version = "0.41.0"

    val modules = Seq(
      "com.dimafeng" %% "testcontainers-scala-scalatest"  % version % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % version % Test
    )

  }

  object doobie {

    val version = "1.0.0-RC2"

    val modules = Seq(
      "org.tpolecat" %% "doobie-core"      % version,
      "org.tpolecat" %% "doobie-hikari"    % version,
      "org.tpolecat" %% "doobie-postgres"  % version,
      "org.tpolecat" %% "doobie-scalatest" % version % Test
    )

  }

  object tofu {

    val version = "0.12.0.1"

    val modules = Seq(
      "tf.tofu" %% "tofu-logging"                  % version,
      "tf.tofu" %% "tofu-logging-derivation"       % version,
      "tf.tofu" %% "tofu-logging-layout"           % version,
      "tf.tofu" %% "tofu-logging-logstash-logback" % version,
      "tf.tofu" %% "tofu-logging-structured"       % version,
      "tf.tofu" %% "tofu-core-ce3"                 % version,
      "tf.tofu" %% "tofu-doobie-logging-ce3"       % version,
      "tf.tofu" %% "derevo-circe"                  % "0.13.0"
    )

  }

  object tapir {

    val version = "1.4.0"

    val modules = Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core"              % version,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % version,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % version,
      "com.softwaremill.sttp.tapir" %% "tapir-derevo"            % version,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % version,
      "com.softwaremill.sttp.tapir" %% "tapir-cats-effect"       % version,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server"  % version % Test
    )

  }

  object tsec {

    val version = "0.4.0"

    val modules = Seq(
      "io.github.jmcardon" %% "tsec-common"   % version,
      "io.github.jmcardon" %% "tsec-password" % version,
      "io.github.jmcardon" %% "tsec-jwt-mac"  % version
    )

  }

}
