import Dependencies._

val appVersion = sys.env.get("TRAVIS_TAG") orElse sys.env.get("BUILD_LABEL") getOrElse s"1.0.0-${System.currentTimeMillis / 1000}-SNAPSHOT"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.indix",
      scalaVersion := "2.11.11",
      version      := appVersion
    )),
    name := "mlflow-gocd",
    libraryDependencies ++= Seq(goPluginLibrary, gson, apacheCommons, httpClient, scalaTest),
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
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
  )
