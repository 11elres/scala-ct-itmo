import Dependencies._

ThisBuild / scalaVersion     := "3.3.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"

Compile / compile / scalacOptions ++= Seq(
  "-Werror",
  "-Wvalue-discard",
  "-unchecked",
)

lazy val root = (project in file("."))
  .settings(
    name := "hw6",
  )
