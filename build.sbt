name := "GSOC Scala PDE solver"

version := "0.1"

// scala version used for this project
scalaVersion := "2.10.2"

scalaSource in Compile <<= baseDirectory(_ / "src/main")

scalaSource in Test <<= baseDirectory(_ / "src/test")

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

fork := true

javaOptions += "-Djava.library.path=lib/"

javaOptions += "-XX:+UseConcMarkSweepGC"

org.scalastyle.sbt.ScalastylePlugin.Settings

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
