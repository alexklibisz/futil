
lazy val root = project.in(file("."))
  .aggregate(futil)
  .settings(
    name := "futil-root",
    publishArtifact := false
  )

lazy val scalaVersions = List("2.12.12", "2.13.5")

lazy val futil = project.in(file("futil"))
  .settings(
    name := "futil",
    description := "Zero-dependency utilities for Scala Futures",
    version := "0.0.2",
    organization := "com.klibisz.elastiknn",
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
    parallelExecution in Test := false,

    // Sonatype bullshit.
    publishTo := sonatypePublishToBundle.value,
    sonatypeProfileName := "com.klibisz.elastiknn",
    publishMavenStyle := true,
    licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage := Some(url("https://github.com/alexklibisz/futil")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/alexklibisz/futil"),
        "scm:git@github.com:alexklibisz/futil.git"
      )
    ),
    developers := List(
      Developer(id="alexklibisz", name="Alex Klibisz", email="aklibisz@gmail.com", url=url("https://alexklibisz.com"))
    )
  )

//lazy val docs = project.in(file("docs"))
//  .enablePlugins(MdocPlugin)
//  .dependsOn(futil)
//  .settings(
//    crossScalaVersions := scalaVersions,
//    publishArtifact := false,
//    // mdoc is only used to "compile" the readme.
//    mdocIn := file("README.md"),
//    mdocOut := file("/dev/null")
//  )
