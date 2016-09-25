package org.github.dragos.vscode

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

import scala.collection.mutable

import org.ensime.api._
import org.ensime.api.TypecheckFileReq
import org.ensime.config.EnsimeConfigProtocol
import org.ensime.core.ShutdownRequest

import com.google.common.base.Charsets
import com.google.common.io.Files

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import langserver.core.LanguageServer
import langserver.messages._
import langserver.types._
import scalariform.formatter.preferences.FormattingPreferences
import scala.io.Source

class EnsimeLanguageServer(in: InputStream, out: OutputStream) extends LanguageServer(in, out) {
  private val system = ActorSystem("ENSIME")

  private val openFiles = mutable.HashSet.empty[String]

  private var ensimeProject: ActorRef = _

  override def initialize(pid: Long, rootPath: String, capabilities: ClientCapabilities): ServerCapabilities = {
    logger.info(s"Initialized with $pid, $rootPath, $capabilities")
    val rootFile = new File(rootPath)
    val cacheDir = new File(rootFile, ".ensime-vscode-cache")
    cacheDir.mkdir()
    val noConfig = EnsimeConfig(
      rootFile,
      cacheDir,
      javaHome = new File(scala.util.Properties.javaHome),
      name = "scala",
      scalaVersion = "2.11.8",
      compilerArgs = Nil,
      referenceSourceRoots = Nil,
      subprojects = Nil,
      formattingPrefs = FormattingPreferences(),
      sourceMode = false,
      javaLibs = Nil)

    val ensimeFile = new File(s"$rootPath/.ensime")
    val config: EnsimeConfig = try {
      EnsimeConfigProtocol.parse(Files.toString(ensimeFile, Charsets.UTF_8))
    } catch {
      case e: Throwable =>
        connection.showMessage(MessageType.Error, s"There was a problem parsing $ensimeFile ${e.getMessage}")
        noConfig
    }
    //showMessage(MessageType.Info, s"Using configuration: $ensimeFile")
    logger.info(s"Using configuration: $config")

    ensimeProject = system.actorOf(Props(classOf[EnsimeProjectServer], this, config))

    // we don't give a damn about them, but Ensime expects it
    ensimeProject ! ConnectionInfoReq

    ServerCapabilities(completionProvider = Some(CompletionOptions(false, Seq("."))))
  }

  override def onOpenTextDocument(td: TextDocumentItem) = {
    openFiles += td.uri

    val f = new File(new URI(td.uri))
    ensimeProject ! TypecheckFileReq(SourceFileInfo(f, Some(td.text)))
  }

  override def onChangeTextDocument(td: VersionedTextDocumentIdentifier, changes: Seq[TextDocumentContentChangeEvent]) = {
    // we assume full text sync
    assert(changes.size == 1)
    val change = changes.head
    assert(change.range.isEmpty)
    assert(change.rangeLength.isEmpty)

    ensimeProject ! TypecheckFileReq(toSourceFileInfo(td.uri, Some(change.text)))
  }

  override def onSaveTextDocument(td: TextDocumentIdentifier) = {
    logger.debug(s"saveTextDocuemnt $td")
  }

  override def onCloseTextDocument(td: TextDocumentIdentifier) = {
    openFiles -= td.uri
  }

  def publishDiagnostics(diagnostics: List[Note]) = {
    val byFile = diagnostics.groupBy(_.file)

    logger.info(s"Received ${diagnostics.size} notes.")

    for {
      file <- openFiles
      path = new URI(file).getRawPath()
    } connection.publishDiagnostics(file, byFile.get(path).toList.flatten.map(toDiagnostic))
  }

  override def shutdown() {
    logger.info("Shutdown request")
    ensimeProject ! ShutdownRequest("Requested by client")
  }

  override def completionRequest(textDocument: TextDocumentIdentifier, position: Position): ResultResponse = {
    ensimeProject ! CompletionsReq(
      toSourceFileInfo(textDocument.uri),
      positionToOffset(contents, position),
      100, caseSens = false, reload = false)
    CompletionList(isIncomplete = false, Nil)
  }

  private def toSourceFileInfo(uri: String, contents: Option[String] = None): SourceFileInfo = {
    val f = new File(new URI(uri))
    SourceFileInfo(f, contents)
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

  private def positionToOffset(contents: String, pos: Position): Int = {
    val source = Source.fromString(contents).getLines()
    var offset = 0
    var line = pos.line
    while (source.hasNext && line > 0) {
      val str = source.next()
      offset += str.length() + 1 // we assume Linux EOLs
    }
    offset + pos.character
  }
}
