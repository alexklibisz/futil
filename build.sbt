
lazy val root = project.in(file("."))
  .aggregate(futil, docs)
  .settings(
    name := "futil-root",
    publishArtifact := false
  )

lazy val scalaVersions = List("2.12.12", "2.13.5")

lazy val futil = project.in(file("futil"))
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

lazy val docs = project.in(file("docs"))
  .enablePlugins(MdocPlugin)
  .dependsOn(futil)
  .settings(
    crossScalaVersions := scalaVersions,
    publishArtifact := false,
    // mdoc is only used to "compile" the readme.
    mdocIn := file("README.md"),
    mdocOut := file("/dev/null")
  )
