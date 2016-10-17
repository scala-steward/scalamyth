lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-target:jvm-1.8"
  )
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    organization := "org.mythtv",
    version := "0.1.0",
    name := "mythtv"
  )

logBuffered in Test := false

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"
