package org.github.dragos.vscode.ensime

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

import org.ensime.api._
import org.ensime.api.EnsimeConfig
import org.ensime.core.Broadcaster
import org.ensime.core.Project
import org.ensime.api.EnsimeServerConfig
import com.typesafe.scalalogging.LazyLogging

import akka.actor.Actor
import akka.util.Timeout
import langserver.core.Connection
import langserver.types._
import java.net.URI
import langserver.messages.MessageType
import org.github.dragos.vscode.EnsimeLanguageServer
import akka.actor.TypedActor.PostStop

class EnsimeProjectServer(langServer: EnsimeLanguageServer, 
                          implicit val config: EnsimeConfig, 
                          implicit val ensimeServerConfig: EnsimeServerConfig) extends Actor with LazyLogging {
  implicit val timeout: Timeout = Timeout(10 seconds)

  val broadcaster = context.actorOf(Broadcaster(), "broadcaster")
  val project = context.actorOf(Project(broadcaster), "project")

  override def preStart() {
    broadcaster ! Broadcaster.Register
  }

  override def postStop() {
    super.postStop()
    logger.info("Shutting down.")
  }

  private val compilerDiagnostics: ListBuffer[Note] = ListBuffer.empty

  override def receive = {
    case ClearAllScalaNotesEvent =>
      compilerDiagnostics.clear()

    case NewScalaNotesEvent(isFull, notes) =>
      compilerDiagnostics ++= notes
      publishDiagnostics()

    case AnalyzerReadyEvent =>
      logger.info("Analyzer is ready!")

    case FullTypeCheckCompleteEvent =>
      logger.info("Full typecheck complete event")
      langServer.publishDiagnostics(compilerDiagnostics.toList)

    case message =>
      logger.debug(s"Forwarding $message")
      project forward message
  }

  private def publishDiagnostics(): Unit = {
    logger.debug(s"Scala notes: ${compilerDiagnostics.mkString("\n")}")

    langServer.publishDiagnostics(compilerDiagnostics.toList)
  }
}
