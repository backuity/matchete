
lazy val commonSettings = Seq(
  organization := "org.backuity",
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.11.8", "2.12.1"),
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
)

lazy val root = project.in(file(".")).
  settings(releaseSettings : _*).
  settings(
    publishArtifact := false
  ).
  aggregate(core,junit,json,xml,macros)

lazy val core = project.in(file("core")).
  settings(commonSettings : _*).
  settings(
    name := "matchete-core",

    libraryDependencies ++= Seq(
      "com.novocode"           %  "junit-interface"       % "0.10"      % "test-internal",          
      "junit"                  %  "junit"                 % "4.10"      % "test",

      // provide illTyped
      "com.chuusai"           %% "shapeless"              % "2.3.2"     % "test")
  ).
  settings(releaseSettings : _*).
  dependsOn(macros)

lazy val junit = project.in(file("junit")).
  settings(commonSettings : _*).
    settings(
      name := "matchete-junit",

      libraryDependencies ++= Seq(
        "com.novocode"           %  "junit-interface"       % "0.10"      % "test-internal",
        "junit"                  %  "junit"                 % "4.10")
    ).
    settings(releaseSettings : _*).
    dependsOn(core)

lazy val json = project.in(file("json")).
  settings(commonSettings : _*).
    settings(
      name := "matchete-json",

      libraryDependencies ++= Seq(
        "com.novocode"           %  "junit-interface"       % "0.10"      % "test-internal",
        "junit"                  %  "junit"                 % "4.10"      % "test",
        "org.json4s"             %% "json4s-native"         % "3.5.0")
    ).
    settings(releaseSettings : _*).
    dependsOn(core,junit)

lazy val xml = project.in(file("xml")).
  settings(commonSettings : _*).
    settings(
      name := "matchete-xml",

      libraryDependencies ++= Seq(
        "com.novocode"           %  "junit-interface"       % "0.10"      % "test-internal",
        "junit"                  %  "junit"                 % "4.10"      % "test",
        "org.scala-lang.modules" %% "scala-xml"             % "1.0.6")
    ).
    settings(releaseSettings : _*).
    dependsOn(core,junit)

// provides Diffable
lazy val macros = project.in(file("macros")).
  settings(commonSettings : _*).
  settings(
    name := "matchete-macros",

    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "com.chuusai" %% "shapeless" % "2.3.2")
  ).
  settings(releaseSettings : _*)
