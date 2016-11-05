package org.github.dragos.vscode

import com.typesafe.scalalogging.LazyLogging
import scala.util.Properties
import java.io.PrintStream
import java.io.FileOutputStream

object Main extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val cwd = System.getenv("PWD")
    logger.info(s"Starting server in $cwd")
    logger.info(s"Classpath: ${Properties.javaClassPath}")

    val server = new EnsimeLanguageServer(System.in, System.out)

    // route System.out somewhere else. The presentation compiler may spit out text
    // and that confuses VScode, since stdout is used for the language server protocol
    val origOut = System.out
    try {
      System.setOut(new PrintStream(new FileOutputStream(s"$cwd/pc.stdout.log")))
      println("This file contains stdout from the presentation compiler.")
      server.start()
    } finally {
      System.setOut(origOut)
    }

    // make sure we actually exit
    System.exit(0)
  }
}
