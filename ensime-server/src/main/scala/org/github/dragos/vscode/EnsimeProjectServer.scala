package org.github.dragos.vscode

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

import org.ensime.api._
import org.ensime.api.EnsimeConfig
import org.ensime.core.Broadcaster
import org.ensime.core.Project

import com.typesafe.scalalogging.LazyLogging

import akka.actor.Actor
import akka.util.Timeout
import langserver.core.Connection
import langserver.types._
import java.net.URI
import langserver.messages.MessageType

class EnsimeProjectServer(connection: Connection, implicit val config: EnsimeConfig) extends Actor with LazyLogging {
  implicit val timeout: Timeout = Timeout(10 seconds)

  val broadcaster = context.actorOf(Broadcaster(), "broadcaster")
  val project = context.actorOf(Project(broadcaster), "project")

  override def preStart() {
    broadcaster ! Broadcaster.Register
  }

  val compilerDiagnostics: ListBuffer[Note] = ListBuffer.empty

  override def receive = {
    case ClearAllScalaNotesEvent =>
      compilerDiagnostics.clear()

    case NewScalaNotesEvent(isFull, notes) =>
      compilerDiagnostics ++= notes
      publishDiagnostics()

    case AnalyzerReadyEvent =>
      logger.info("Analyzer is ready!")
      connection.showMessage(MessageType.Info, "Ensime is ready")

    case FullTypeCheckCompleteEvent =>
      logger.info("Full typecheck complete event")

    case message =>
      project forward message
  }

  private def publishDiagnostics(): Unit = {
    logger.debug(s"Scala notes: ${compilerDiagnostics.mkString("\n")}")

    for ((file, notes) <- compilerDiagnostics.groupBy(_.file))
      connection.publishDiagnostics(s"file://$file", notes.map(toDiagnostic))
  }

  private def toDiagnostic(note: Note): Diagnostic = {
    val start: Int = note.beg
    val end: Int = note.end
    val length = end - start

    val severity = note.severity match {
      case NoteError => DiagnosticSeverity.Error
      case NoteWarn => DiagnosticSeverity.Warning
      case NoteInfo => DiagnosticSeverity.Information
    }

    // Scalac reports 1-based line and columns, while Code expects 0-based
    val range = Range(Position(note.line - 1, note.col - 1), Position(note.line - 1, note.col - 1 + length))

    Diagnostic(range, Some(severity), code = None, source = Some("Scala"), message = note.msg)
  }
}
