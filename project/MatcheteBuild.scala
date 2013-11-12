import sbt._
import Keys._
import sbtrelease.ReleasePlugin

object MatcheteBuild extends Build {

  lazy val main = project.in(file("."))
    .settings(
      name := "matchete",
      organization := "org.backuity",
      scalaVersion := "2.10.2",

      homepage := Some(url("https://github.com/backuity/matchete")),
      licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),

      scalacOptions ++= Seq("-deprecation", "-unchecked"),

      libraryDependencies ++= Seq(
        "com.novocode"          % "junit-interface"       % "0.10"      % "test",
        "junit"                 % "junit"                 % "4.10"      % "optional"),

      publishMavenStyle := true,

      publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (version.value.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases"  at nexus + "service/local/staging/deploy/maven2")
      },

      pomIncludeRepository := { _ => false },

      pomExtra :=
        <scm>
          <url>git@github.com:backuity/matchete.git</url>
          <connection>scm:git:git@github.com:backuity/matchete.git</connection>
        </scm>
        <developers>
          <developer>
            <id>backuitist</id>
            <name>Bruno Bieth</name>
            <url>https://github.com/backuitist</url>
          </developer>
        </developers>,

      // do not publish documentation -- source is enough
      publishArtifact in packageDoc := false
    )
    .settings(ReleasePlugin.releaseSettings : _*)
    .dependsOn(testMacro % "test->compile")

  lazy val testMacro = project.in(file("test-macro")).settings(
    libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.10.1"
  )
}