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
      "org.scalatest" %% "scalatest" % "2.2.6" % "test"
    )
  )
