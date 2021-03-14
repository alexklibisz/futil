import com.jsuereth.sbtpgp.PgpKeys._
import sbtrelease.ReleaseStateTransformations._

// TODO: change this to com.klibisz.futil once OSSRH provisioning is complete.
// Needs to be Global, else sbt-sonatype and sonatype are unhappy.
Global / organization := "com.klibisz.elastiknn"

// To set the version, just strip -SNAPSHOT from the version.
// "1.2.3-PRE1-SNAPSHOT" -> "1.2.3-PRE1"
releaseVersion := { _.replace("-SNAPSHOT", "") }

// To set the next version, increment the last number and append -SNAPSHOT.
// "1.2.3-PRE1" -> "1.2.3-PRE2-SNAPSHOT"
releaseNextVersion := { v: String =>
  "[0-9]+".r
    .findAllMatchIn(v)
    .toList
    .lastOption
    .map(m => v.take(m.start) ++ s"${m.toString.toInt + 1}" ++ v.drop(m.`end`) + "-SNAPSHOT")
    .getOrElse(v)
}

// Slightly modified to work with sbt-sonatype.
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

lazy val scalaVersions = List("2.12.12", "2.13.5")

lazy val noPublishSettings = Seq(
  publish,
  publishArtifact,
  publishSigned,
  publishConfiguration,
  sonatypeBundleRelease
).map(skip in _ := true)

lazy val root = project.in(file("."))
  .aggregate(futil, docs)
  .settings(
    name := "futil-root",
    noPublishSettings
  )

lazy val futil = project.in(file("futil"))
  .settings(
    name := "futil",
    description := "Zero-dependency utilities for Scala Futures",
    crossScalaVersions := scalaVersions,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.5" % Test,
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

    // sbt-sonatype settings
    publishTo := sonatypePublishToBundle.value,
    publishMavenStyle := true,
    licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage := Some(url("https://github.com/alexklibisz/futil")),
    scmInfo := Some(ScmInfo(url("https://github.com/alexklibisz/futil"), "scm:git@github.com:alexklibisz/futil.git")),
    developers += Developer(id="alexklibisz", name="Alex Klibisz", email="aklibisz@gmail.com", url=url("https://alexklibisz.com")),
  )

lazy val docs = project.in(file("docs"))
  .enablePlugins(MdocPlugin)
  .dependsOn(futil)
  .settings(
    noPublishSettings,
    crossScalaVersions := scalaVersions,
    mdocIn := file("README.md"),
    mdocOut := file("/dev/null")
  )
