import Dependencies.*

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version      := "0.1.0-SNAPSHOT"

Compile / scalacOptions ++= Seq(
  "-Werror",
  "-Wdead-code",
  "-Wextra-implicit",
  "-Wnumeric-widen",
  "-Wunused",
  "-Wvalue-discard",
  "-Xlint",
  "-Xlint:-byname-implicit",
  "-Xlint:-implicit-recursion",
  "-unchecked",
  "-feature"
)

// sql tests launch in class with common db container
Test / testOptions += Tests.Filter(test =>
  !test.endsWith("RepositorySqlSpec")
)

enablePlugins(
  JavaAppPackaging,
  DockerPlugin
)

Compile / mainClass  := Some("Server")
Docker / packageName := "social-network"
dockerExposedPorts ++= Seq(1234)
dockerBaseImage := "openjdk:8-jre-slim"

lazy val root = (project in file("."))
  .settings(
    name := "SimpleSocialNetwork",
    libraryDependencies ++= Seq(
      `cats-effect`,
      client3,
      newtype,
      pureconfig,
      ember,
      scalatest,
      mockito,
      postgres
    ) ++ testcontainers.modules ++ doobie.modules ++ tofu.modules ++ tapir.modules ++ tsec.modules,
    dependencyOverrides ++= {
      val circeVersion = "0.14.3"
      Seq(
        "io.circe" %% "circe-core"    % circeVersion,
        "io.circe" %% "circe-generic" % circeVersion,
        "io.circe" %% "circe-parser"  % circeVersion,
        "io.circe" %% "circe-jawn"    % circeVersion
      )
    },
    scalacOptions += "-Ymacro-annotations",
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
  )
