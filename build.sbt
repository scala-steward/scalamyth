lazy val commonSettings = Seq(
  organization := "org.mythtv",
  version := "0.1.0",
  scalaVersion := "2.11.8",
  scalacOptions += "-target:jvm-1.8"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "mythtv"
  )

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.2"
