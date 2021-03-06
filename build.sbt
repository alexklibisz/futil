
lazy val scalaVersions = List("2.12.12", "2.13.5")

lazy val futil = project.in(file("."))
  .settings(
    name := "futil",
    description := "Zero-dependency utilities for Scala Futures",
    organization := "com.klibisz.futil",
    version := "0.0.1",
    crossScalaVersions := scalaVersions,
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
    parallelExecution in Test := false
  )