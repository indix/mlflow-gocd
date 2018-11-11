import Dependencies._

val appVersion = sys.env.get("TRAVIS_TAG") orElse sys.env.get("BUILD_LABEL") getOrElse s"1.0.0-${System.currentTimeMillis / 1000}-SNAPSHOT"

lazy val commonSettings = Seq(
  organization := "com.indix",
  version := appVersion,
  scalaVersion := "2.11.11",
  unmanagedBase := file(".") / "lib",
  libraryDependencies ++= Seq(
    goPluginLibrary, gson, apacheCommons, commonsIo, scalaTest
  ),
  resourceGenerators in Compile += Def.task {
    val inputFile = baseDirectory.value / "template" / "plugin.xml"
    val outputFile = (resourceManaged in Compile).value / "plugin.xml"
    val contents = IO.read(inputFile)
    IO.write(outputFile, contents.replaceAll("\\$\\{version\\}", appVersion))
    Seq(outputFile)
  }.taskValue,
  mappings in (Compile, packageBin) += {
   (resourceManaged in Compile).value / "plugin.xml" -> "plugin.xml"
  },
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  crossPaths := false,
  assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
)

lazy val material = (project in file("material")).
  settings(commonSettings: _*).
  settings(
    name := "mlflow-gocd-material",
    libraryDependencies ++= Seq(httpClient),
  )

lazy val fetch = (project in file("fetch")).
  settings(commonSettings: _*).
  settings(
    name := "mlflow-gocd-fetch",
    libraryDependencies ++= Seq(awsS3),
  )

lazy val root = Project(
  id = "mlflow-gocd",
  base = file(".")
) aggregate(material, fetch)
