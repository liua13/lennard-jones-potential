// See README.md for license details.

ThisBuild / scalaVersion     := "2.13.10" //"2.13.8"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "%ORGANIZATION%"

val chiselVersion = "3.5.6"

lazy val root = (project in file("."))
  .settings(
    name := "%NAME%",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      // "edu.berkeley.cs" %% "chisel-iotesters" % "2.5.6",
      "edu.berkeley.cs" %% "chiseltest" % "0.5.4" % "test",
      "edu.berkeley.cs" %% "hardfloat" % "1.5-SNAPSHOT"
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-P:chiselplugin:genBundleElements",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
  )

// version := "0.1.0"
// scalaVersion     := "2.13.10" //"2.13.8"

// libraryDependencies ++= Seq(
//   "edu.berkeley.cs" %% "chisel3" % "3.5.6",
//   "edu.berkeley.cs" %% "rocketchip" % "1.2.+",
//   "edu.berkeley.cs" %% "chisel-iotesters" % "2.5.6",
//   "edu.berkeley.cs" %% "hardfloat" % "1.5-SNAPSHOT"
// )

// resolvers ++= Seq(
//   Resolver.sonatypeOssRepos("snapshots"),
//   Resolver.sonatypeOssRepos("releases"),
//   Resolver.mavenLocal
// )
