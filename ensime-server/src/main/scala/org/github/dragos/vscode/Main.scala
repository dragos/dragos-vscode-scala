package org.github.dragos.vscode

import com.typesafe.scalalogging.LazyLogging
import scala.util.Properties

object Main extends LazyLogging {
  def main(args: Array[String]): Unit = {
    logger.info(s"Starting server in ${System.getenv("PWD")}")
    logger.info(s"Classpath: ${Properties.javaClassPath}")

    val server = new EnsimeLanguageServer(System.in, System.out)
    server.start()
  }
}
