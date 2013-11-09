import sbt._
import Keys._
import sbtrelease.ReleasePlugin

object MatcheteBuild extends Build {

  lazy val main = project.in(file("."))
    .settings(
      name := "matchete",
      organization := "org.backuity",
      scalaVersion := "2.10.2",

      scalacOptions ++= Seq("-deprecation", "-unchecked"),

      libraryDependencies ++= Seq(
        "com.novocode"          % "junit-interface"       % "0.10"      % "test",
        "junit"                 % "junit"                 % "4.10"      % "optional"),

      publishMavenStyle := true,

      // do not publish documentation -- source is enough
      publishArtifact in packageDoc := false
    )
    .settings(ReleasePlugin.releaseSettings : _*)
    .dependsOn(testMacro % "test->compile")

  lazy val testMacro = project.in(file("test-macro")).settings(
    libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.10.1"
  )
}