package org.github.dragos.vscode

import java.io.File
import java.io.InputStream
import java.io.OutputStream

import org.ensime.api._
import org.ensime.config.EnsimeConfigProtocol

import com.google.common.base.Charsets
import com.google.common.io.Files

import akka.actor.ActorSystem
import langserver.core.LanguageServer
import langserver.messages._
import langserver.types.TextDocumentIdentifier
import langserver.types.TextDocumentContentChangeEvent
import langserver.types.VersionedTextDocumentIdentifier
import langserver.types.TextDocumentItem
import akka.actor.ActorRef
import akka.actor.Props
import org.ensime.api.TypecheckFileReq
import java.net.URI
import scalariform.formatter.preferences.FormattingPreferences
import org.ensime.core.ShutdownRequest

class EnsimeLanguageServer(in: InputStream, out: OutputStream) extends LanguageServer(in, out) {
  private val system = ActorSystem("ENSIME")

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

    ensimeProject = system.actorOf(Props(classOf[EnsimeProjectServer], connection, config))

    // we don't give a damn about them, but Ensime expects it
    ensimeProject ! ConnectionInfoReq
    ServerCapabilities(completionProvider = Some(CompletionOptions(false, Seq("."))))
  }

  override def onOpenTextDocument(td: TextDocumentItem) = {
    logger.debug(s"openTextDocuemnt $td")

    val f = new File(new URI(td.uri))
    ensimeProject ! TypecheckFileReq(SourceFileInfo(f, Some(td.text)))
  }

  override def onChangeTextDocument(td: VersionedTextDocumentIdentifier, changes: Seq[TextDocumentContentChangeEvent]) = {
    logger.debug(s"changeTextDocuemnt $td")

    val f = new File(new URI(td.uri))

    // we assume full text sync
    assert(changes.size == 1)
    val change = changes.head
    assert(change.range.isEmpty)
    assert(change.rangeLength.isEmpty)

    ensimeProject ! TypecheckFileReq(SourceFileInfo(f, Some(change.text)))
  }

  override def onSaveTextDocument(td: TextDocumentIdentifier) = {
    logger.debug(s"saveTextDocuemnt $td")
  }

  override def onCloseTextDocument(td: TextDocumentIdentifier) = {
    logger.debug(s"closeTextDocuemnt $td")
  }

  override def shutdown() {
    logger.info("Shutdown request")
    ensimeProject ! ShutdownRequest("Requested by client")
  }
}
