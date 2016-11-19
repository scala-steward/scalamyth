lazy val commonSettings = Seq(
  scalaVersion := "2.12.0",
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-Xlint"
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

libraryDependencies += "net.straylightlabs" % "hola" % "0.2.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"
