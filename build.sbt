name := "futil"

description := "Utilities to get more from Scala Futures"

organization := "com.alexklibisz"

version := "0.1"

scalaVersion := "2.12.12"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.5" % "test"
)

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-deprecation",
  "-unchecked",
  "-Xlint:unused"
)

javaOptions in test ++= Seq("-Xms768m", "-Xmx768m")

parallelExecution in test := false