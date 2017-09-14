name := "vscode-scala"


scalaVersion in ThisBuild := "2.12.3"

publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }

lazy val commonSettings = Seq(
  organization := "com.github.dragos",
  version := "0.1.7-SNAPSHOT",
  resolvers += "dhpcs at bintray" at "https://dl.bintray.com/dhpcs/maven",
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test"
  ),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val languageserver = project.
  settings(commonSettings).
  settings(
    libraryDependencies ++= Seq(
      "com.dhpcs" %% "scala-json-rpc" % "2.0.1",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
      "org.slf4j" % "slf4j-api" % "1.7.21",
      "ch.qos.logback" %  "logback-classic" % "1.1.7",
      "org.codehaus.groovy" % "groovy" % "2.4.0"
    ),
    pomExtra := {
      <url>https://github.com/dragos/dragos-vscode-scala/</url>
      <licenses>
        <license>
          <name>Apache 2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
      </licenses>
      <scm>
        <connection>scm:git:github.com/dragos/dragos-vscode-scala.git</connection>
        <developerConnection>scm:git:git@github.com:dragos/dragos-vscode-scala.git</developerConnection>
        <url>github.com/dragos/dragos-vscode-scala.git</url>
      </scm>
      <developers>
        <developer>
          <id>dragos</id>
          <name>Iulian Dragos</name>
          <url>https://github.com/dragos/</url>
        </developer>
      </developers>
    }
  )

lazy val `ensime-lsp` = project.
  in(file("ensime-lsp")).
  dependsOn(languageserver).
  settings(commonSettings).
  settings(
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Seq(
      "org.ensime" %% "core" % "2.0.0-M4"
    ),
    pomExtra in Global := {
      <url>https://github.com/dragos/dragos-vscode-scala/</url>
      <licenses>
        <license>
          <name>GPL3</name>
          <url>https://www.gnu.org/licenses/gpl-3.0.en.html</url>
        </license>
      </licenses>
      <scm>
        <connection>scm:git:github.com/dragos/dragos-vscode-scala.git</connection>
        <developerConnection>scm:git:git@github.com:dragos/dragos-vscode-scala.git</developerConnection>
        <url>github.com/dragos/dragos-vscode-scala.git</url>
      </scm>
      <developers>
        <developer>
          <id>dragos</id>
          <name>Iulian Dragos</name>
          <url>https://github.com/dragos/</url>
        </developer>
      </developers>
    }
  )
