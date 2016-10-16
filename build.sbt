name := "vscode-scala"


scalaVersion in ThisBuild := "2.11.8"

lazy val commonSettings = Seq(
  organization := "com.github.dragos",
  version := "0.1.0",
  resolvers += "dhpcs at bintray" at "https://dl.bintray.com/dhpcs/maven"
)

lazy val languageserver = project.
  settings(commonSettings:_*).
  settings(
    libraryDependencies ++= Seq(
      "com.dhpcs" %% "play-json-rpc" % "1.2.1",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
      "org.slf4j" % "slf4j-api" % "1.7.21",
      "ch.qos.logback" %  "logback-classic" % "1.1.7",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test",
      "org.codehaus.groovy" % "groovy" % "2.4.0"
    )
  )

lazy val ensimeServer = project.
  in(file("ensime-server")).
  dependsOn(languageserver).
  settings(commonSettings:_*).
  settings(
    libraryDependencies ++= Seq(
      "org.ensime" %% "core" % "1.0.0"
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", "commons", "vfs2", xs @ _*) => MergeStrategy.first // assumes our classpath is setup correctly
      case PathList("scala", "reflect", "io", xs @ _*) => MergeStrategy.first // assumes our classpath is setup correctly
      case PathList("logback.groovy", xs @ _*) => MergeStrategy.first // assumes our classpath is setup correctly
      case other => MergeStrategy.defaultMergeStrategy(other)
    }
  )

lazy val publishExtension = taskKey[Unit]("Copy ensimeServer assembly to extension")

publishExtension := {
  val assemblyFile = (assembly in ensimeServer).value
  println(s"""Copying $assemblyFile to ${baseDirectory.value / "scala" / "server"}.""")
  IO.copyFile(assemblyFile, baseDirectory.value / "scala" / "server" / assemblyFile.getName)
}