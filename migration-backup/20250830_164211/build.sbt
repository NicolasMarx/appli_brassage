import sbt._
import sbt.Keys._

ThisBuild / organization := "com.mybrew"
ThisBuild / scalaVersion := "2.13.16"           // SIP-51 ok
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "My-brew-app-V2",

    libraryDependencies ++= Seq(
      guice,
      ws,
      "org.playframework" %% "play-slick"            % "6.2.0",
      "org.playframework" %% "play-slick-evolutions" % "6.2.0",
      "org.postgresql"     % "postgresql"            % "42.7.4"
    ),

    // Verrou SIP-51 (le compilateur n'est jamais plus vieux que scala-library)
    dependencyOverrides ++= Seq(
      "org.scala-lang" % "scala-library"  % scalaVersion.value,
      "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    ),

    scalacOptions ++= Seq(
      "-deprecation", "-feature", "-unchecked",
      "-Xlint:_", "-Ywarn-numeric-widen", "-Ywarn-value-discard"
    ),
    evictionErrorLevel := Level.Warn
  )
libraryDependencies += "de.mkammerer" % "argon2-jvm" % "2.11"
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.4"
