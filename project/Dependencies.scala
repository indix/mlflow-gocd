import sbt._

object Dependencies {
  lazy val goPluginLibrary = "cd.go.plugin" % "go-plugin-api" % "18.10.0" % Provided
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
}
