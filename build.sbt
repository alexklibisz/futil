
lazy val futil = project.in(file("."))
  .enablePlugins(MdocPlugin)
  .settings(
    name := "futil",
    description := "Zero-dependency utilities for Scala Futures",
    organization := "com.klibisz.futil",
    version := "0.0.1",
    crossScalaVersions := List("2.12.12", "2.13.5"),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.5" % Test
    ),
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-deprecation",
      "-unchecked",
      "-Xlint:unused"
    ),
    coverageMinimum := 100,
    coverageFailOnMinimum := true,
    fork in Test := true,
    javaOptions in Test ++= Seq("-Xms768m", "-Xmx768m"),
    parallelExecution in Test := false,

    // mdoc is only used to "compile" the readme.
    mdocIn := file("README.md"),
    mdocOut := file("/dev/null")
  )
