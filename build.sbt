name := "vscode-scala"

lazy val commonSettings = Seq(
  organization := "com.github.dragos",
  version := "0.1.0",
  scalaVersion := "2.11.8",
  resolvers += "dhpcs at bintray" at "https://dl.bintray.com/dhpcs/maven"
)

lazy val languageserver = project.
  settings(commonSettings:_*).
  settings(
    libraryDependencies ++= Seq(
      "com.dhpcs" %% "play-json-rpc" % "1.0.0",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
      "org.slf4j" % "slf4j-api" % "1.7.21",
      "ch.qos.logback" %  "logback-classic" % "1.1.7",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test",
      "org.codehaus.groovy" % "groovy" % "2.4.0"
    )
  )
