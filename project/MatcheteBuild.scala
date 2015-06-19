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
import sbt.Keys._
import sbtrelease.ReleasePlugin

object MatcheteBuild extends Build {

  lazy val commonSettings = Seq(
    organization := "org.backuity",
    scalaVersion := "2.11.6",

    scalacOptions ++= Seq("-deprecation", "-unchecked")
  )
  
  lazy val releaseSettings = Seq(
    homepage := Some(url("https://github.com/backuity/matchete")),
    licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),

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
  ) ++ ReleasePlugin.releaseSettings

  lazy val main = project.in(file("."))
    .settings(commonSettings : _*)
    .settings(
      name := "matchete",

      libraryDependencies ++= Seq(
        "com.novocode"           %  "junit-interface"       % "0.10"      % "test-internal",
        "org.scala-lang.modules" %% "scala-xml"             % "1.0.2"     % "optional",
        "junit"                  %  "junit"                 % "4.10"      % "optional")
    )
    .settings(releaseSettings : _*)
    .dependsOn(testMacro % "test-internal->compile")
    .dependsOn(macros)
    .aggregate(macros)

  // macros need to be compiled separately

  // provides illTyped
  lazy val testMacro = project.in(file("test-macro"))
    .settings(commonSettings : _*)
    .settings(
      libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )

  // provides Diffable
  lazy val macros = project.in(file("macros"))
    .settings(commonSettings : _*)
    .settings(
      name := "matchete-macros",

      libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
    .settings(releaseSettings : _*)
}