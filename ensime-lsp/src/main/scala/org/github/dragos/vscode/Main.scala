package org.github.dragos.vscode

import com.typesafe.scalalogging.LazyLogging
import scala.util.Properties
import java.io.PrintStream
import java.io.FileOutputStream

object Main extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val cwd = System.getProperty("vscode.workspace")
    logger.info(s"Starting server in $cwd")
    logger.info(s"Classpath: ${Properties.javaClassPath}")
    logger.info(s"LogLevel: ${Option(System.getProperty("vscode.logLevel")).getOrElse("")}")

    val server = new EnsimeLanguageServer(System.in, System.out)

    // route System.out somewhere else. The presentation compiler may spit out text
    // and that confuses VScode, since stdout is used for the language server protocol
    val origOut = System.out
    try {
      System.setOut(new PrintStream(new FileOutputStream(s"$cwd/pc.stdout.log")))
      System.setErr(new PrintStream(new FileOutputStream(s"$cwd/pc.stdout.log")))
      println("This file contains stdout from the presentation compiler.")
      server.start()
    } finally {
      System.setOut(origOut)
    }

    logger.underlying match {
      case logbackLogger:ch.qos.logback.classic.Logger => 
        val logLevel = Option(System.getProperty("vscode.logLevel"))
        logLevel match{
          case Some("ERROR") => logbackLogger.setLevel(ch.qos.logback.classic.Level.ERROR)
          case Some("INFO") => logbackLogger.setLevel(ch.qos.logback.classic.Level.INFO)
          case Some("DEBUG") => logbackLogger.setLevel(ch.qos.logback.classic.Level.DEBUG)
          case Some("WARN") => logbackLogger.setLevel(ch.qos.logback.classic.Level.WARN)
          case _ =>
        }
      case _ =>
    }

    // make sure we actually exit
    System.exit(0)
  }
}
