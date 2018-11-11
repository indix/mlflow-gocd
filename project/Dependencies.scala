import sbt._

object Dependencies {
  lazy val goPluginLibrary = "cd.go.plugin" % "go-plugin-api" % "17.2.0" % Provided
  lazy val apacheCommons = "org.apache.commons" % "commons-lang3" % "3.1"
  lazy val gson = "com.google.code.gson" % "gson" % "2.2.3"
  lazy val httpClient = "com.google.http-client" % "google-http-client-gson" % "1.19.0"
  lazy val commonsIo = "commons-io" % "commons-io" % "1.3.2"
  lazy val awsS3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.127"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
}
