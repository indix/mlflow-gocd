import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.indix",
      scalaVersion := "2.11.11",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "mlflow-gocd",
    libraryDependencies ++= Seq(goPluginLibrary, scalaTest)
  )
