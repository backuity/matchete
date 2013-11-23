/*
 * Copyright 2013 Bruno Bieth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.typesafe.sbt.pgp.PgpKeys
import sbt._
import Keys._
import sbtrelease.ReleasePlugin

object MatcheteBuild extends Build {

  override def settings = super.settings ++ Seq(
    scalaVersion := "2.10.3"
  )

  lazy val main = project.in(file("."))
    .settings(
      name := "matchete",
      organization := "org.backuity",

      homepage := Some(url("https://github.com/backuity/matchete")),
      licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),

      scalacOptions ++= Seq("-deprecation", "-unchecked"),

      libraryDependencies ++= Seq(
        "com.novocode"          % "junit-interface"       % "0.10"      % "test-internal",
        "junit"                 % "junit"                 % "4.10"      % "optional"),

      publishMavenStyle := true,

      publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (version.value.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases"  at nexus + "service/local/staging/deploy/maven2")
      },

      // replace publish by publishSigned
      publish := PgpKeys.publishSigned.value,

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
        </developers>
    )
    .settings(ReleasePlugin.releaseSettings : _*)
    .dependsOn(testMacro % "test-internal->compile")

  // macro need to be compiled separately
  lazy val testMacro = project.in(file("test-macro")).settings(
    libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.10.3"
  )
}