
lazy val commonSettings = Seq(
  organization := "io.grigg",
  version      := "0.1.0",
  scalaVersion := "2.12.7",

  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-Xlint"
  )
)

lazy val root = (project in file("."))
  .aggregate(scalamyth, examples)

lazy val scalamyth = (project in file("bindings"))
  .settings(commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.0",
      "org.scala-lang.modules"     %% "scala-xml"       % "1.0.6",
      "io.spray"                   %% "spray-json"      % "1.3.2",
      "ch.qos.logback"              % "logback-classic" % "1.2.3",
      "net.straylightlabs"          % "hola"            % "0.2.3",
      "org.scalatest"              %% "scalatest"       % "3.0.0"  % "test",
    )
  )

lazy val examples = (project in file("examples"))
  .dependsOn(scalamyth)
  .settings(commonSettings)

logBuffered in Test := false

