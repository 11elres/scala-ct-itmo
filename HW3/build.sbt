import Dependencies.*

ThisBuild / scalaVersion := "3.3.3"
ThisBuild / version      := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "building-adt",
    libraryDependencies ++= List(
      catsCore,
      scalaTest % Test
    ),
    scalacOptions ++= Seq(
      "-Xsource:3"
    )
  )
