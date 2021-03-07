
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

lazy val docs = project.in(file("docs"))
  .enablePlugins(MicrositesPlugin)
  .settings(
    publishArtifact := false,
    micrositeName := "futil",
    micrositeAuthor := "Alex Klibisz",
    micrositeDescription := "Minimal utilities for Scala Futures",
    micrositeBaseUrl := "/futil",
    micrositeDocumentationUrl := "/futil/docs",
    micrositeHomepage := "https://github.com/alexklibisz/futil",
    micrositeGithubOwner := "alexklibisz",
    micrositeGithubRepo := "futil",
    micrositeTheme := "pattern",
    micrositePushSiteWith := GitHub4s,
    micrositeSearchEnabled := false,
    micrositeFooterText := Some(""),
    mdocIn := (sourceDirectory in Compile).value / "mdoc"
  )
