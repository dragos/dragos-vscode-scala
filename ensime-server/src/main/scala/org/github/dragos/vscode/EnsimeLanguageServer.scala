package org.github.dragos.vscode

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

import org.ensime.api._
import org.ensime.api.TypecheckFileReq
import org.ensime.config.EnsimeConfigProtocol
import org.ensime.core.ShutdownRequest
import org.ensime.util.file._

import com.google.common.base.Charsets
import com.google.common.io.Files

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import langserver.core.LanguageServer
import langserver.messages._
import langserver.types._
//import scalariform.formatter.preferences.FormattingPreferences
import langserver.core.TextDocument
import org.github.dragos.vscode.ensime.EnsimeActor
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import java.nio.charset.Charset
import langserver.core.MessageReader

class EnsimeLanguageServer(in: InputStream, out: OutputStream) extends LanguageServer(in, out) {
  private val system = ActorSystem("ENSIME")
  private var fileStore: TempFileStore = _

  // Ensime root actor
  private var ensimeActor: ActorRef = _
  implicit val timeout = Timeout(5 seconds)

  override def start() {
    super.start()
    // if we got here it means the connection was closed
    // cleanup and exit
    shutdown()
  }

  override def initialize(pid: Long, rootPath: String, capabilities: ClientCapabilities): ServerCapabilities = {
    logger.info(s"Initialized with $pid, $rootPath, $capabilities")

    val rootFile = new File(rootPath)
    val cacheDir = new File(rootFile, ".ensime_cache")
    cacheDir.mkdir()
//    val noConfig = EnsimeConfig(
//      rootFile,
//      cacheDir,
//      javaHome = new File(scala.util.Properties.javaHome),
//      name = "scala",
//      scalaVersion = "2.11.8",
//      compilerArgs = Nil,
//      referenceSourceRoots = Nil,
//      subprojects = Nil,
//      formattingPrefs = FormattingPreferences(),
//      sourceMode = false,
//      javaLibs = Nil)

    initializeEnsime(rootPath)

    ServerCapabilities(
      completionProvider = Some(CompletionOptions(false, Seq("."))),
      definitionProvider = true,
      hoverProvider = true
    )
  }

  private def initializeEnsime(rootPath: String): Try[EnsimeConfig] = {
    val ensimeFile = new File(s"$rootPath/.ensime")

    val configT = Try {
      EnsimeConfigProtocol.parse(Files.toString(ensimeFile, Charsets.UTF_8))
    }

    configT match {
      case Failure(e) =>
        connection.showMessage(MessageType.Error, s"There was a problem parsing $ensimeFile ${e.getMessage}")
      case Success(config) =>
        //showMessage(MessageType.Info, s"Using configuration: $ensimeFile")
        logger.info(s"Using configuration: $config")
        fileStore = new TempFileStore(config.cacheDir.toString)
        ensimeActor = system.actorOf(Props(classOf[EnsimeActor], this, config), "server")

        // we don't give a damn about them, but Ensime expects it
        ensimeActor ! ConnectionInfoReq
    }
    configT
  }

  override def onOpenTextDocument(td: TextDocumentItem) = {
    val f = new File(new URI(td.uri))
    if (f.getAbsolutePath.startsWith(fileStore.path)) {
      logger.debug(s"Not adding temporary file $f to Ensime")
    } else {
      ensimeActor ! TypecheckFileReq(SourceFileInfo(RawFile(f.toPath()), Some(td.text)))
    }
  }

  override def onChangeTextDocument(td: VersionedTextDocumentIdentifier, changes: Seq[TextDocumentContentChangeEvent]) = {
    // we assume full text sync
    assert(changes.size == 1)
    val change = changes.head
    assert(change.range.isEmpty)
    assert(change.rangeLength.isEmpty)

    ensimeActor ! TypecheckFileReq(toSourceFileInfo(td.uri, Some(change.text)))
  }

  override def onSaveTextDocument(td: TextDocumentIdentifier) = {
    logger.debug(s"saveTextDocuemnt $td")
  }

  override def onCloseTextDocument(td: TextDocumentIdentifier) = {
    logger.debug("Removing ${td.uri} from Ensime.")
    val doc = documentManager.documentForUri(td.uri)
    doc.map(d => ensimeActor ! RemoveFileReq(d.toFile))
  }

  def publishDiagnostics(diagnostics: List[Note]) = {
    val byFile = diagnostics.groupBy(_.file)

    logger.info(s"Received ${diagnostics.size} notes.")

    for {
      doc <- documentManager.allOpenDocuments
      path = new URI(doc.uri).getRawPath()
    } connection.publishDiagnostics(doc.uri, byFile.get(path).toList.flatten.map(toDiagnostic))
  }

  override def shutdown() {
    logger.info("Shutdown request")
//    ensimeActor ! ShutdownRequest("Requested by client")
    system.shutdown()
    logger.info("Shutting down actor system.")
    system.awaitTermination()
    logger.info("Actor system down.")
  }

  override def completionRequest(textDocument: TextDocumentIdentifier, position: Position): ResultResponse = {
    import scala.concurrent.ExecutionContext.Implicits._

    val res = for (doc <- documentManager.documentForUri(textDocument.uri)) yield {
      val future = ensimeActor ? CompletionsReq(
        toSourceFileInfo(textDocument.uri, Some(new String(doc.contents))),
        doc.positionToOffset(position),
        100, caseSens = false, reload = false)

      future.onComplete { f => logger.debug(s"Completions future completed: succes? ${f.isSuccess}") }

      future.map {
        case CompletionInfoList(prefix, completions) =>
          logger.debug(s"Received ${completions.size} completions: ${completions.take(10).map(_.name)}")
          completions.sortBy(- _.relevance).map(toCompletion)
      }
    }

    res.map(f => CompletionList(false, Await.result(f, 5 seconds))) getOrElse CompletionList(false, Nil)
  }

  override def gotoDefinitionRequest(textDocument: TextDocumentIdentifier, position: Position): Seq[Location] = {
    import scala.concurrent.ExecutionContext.Implicits._
    logger.info(s"Got goto definition request at (${position.line}, ${position.character}).")

    val res = for (doc <- documentManager.documentForUri(textDocument.uri)) yield {
      val future = ensimeActor ? SymbolAtPointReq(
        Right(toSourceFileInfo(textDocument.uri, Some(new String(doc.contents)))),
        doc.positionToOffset(position))

      future.onComplete { f => logger.debug(s"Goto Definition future completed: succes? ${f.isSuccess}") }

      future.map {
        case SymbolInfo(name, localName, declPos, typeInfo) =>
          declPos.toSeq.flatMap {
            case OffsetSourcePosition(ensimeFile, offset) =>
              fileStore.getFile(ensimeFile).map { path =>
                val file = path.toFile
                val uri = file.toURI.toString
                val doc = TextDocument(uri, file.readString()(MessageReader.Utf8Charset).toCharArray())
                val start = doc.offsetToPosition(offset)
                val end = start.copy(character = start.character + localName.length())

                logger.info(s"Found definition at $uri, line: ${start.line}")
                Seq(Location(uri, Range(start, end)))
              }.recover {
                case e =>
                  logger.error(s"Couldn't retrieve hyperlink target file $e")
                  Seq()
              }.get

            case _ =>
              Seq()
          }
      }
    }

    res.map { f =>  Await.result(f, 5 seconds) } getOrElse Seq.empty[Location]
  }

  override def hoverRequest(textDocument: TextDocumentIdentifier, position: Position) =
    gotoDefinitionRequest(textDocument: TextDocumentIdentifier, position: Position)

  private def toSourceFileInfo(uri: String, contents: Option[String] = None): SourceFileInfo = {
    val f = new File(new URI(uri))
    SourceFileInfo(RawFile(f.toPath), contents)
  }

  private def toCompletion(completionInfo: CompletionInfo) = {
    def symKind: Option[Int] = completionInfo.typeInfo map { info =>
      info.declaredAs match {
        case DeclaredAs.Method => CompletionItemKind.Method
        case DeclaredAs.Class  => CompletionItemKind.Class
        case DeclaredAs.Field  => CompletionItemKind.Field
        case DeclaredAs.Interface | DeclaredAs.Trait => CompletionItemKind.Interface
        case DeclaredAs.Object    => CompletionItemKind.Module
        case _ => CompletionItemKind.Value
      }
    }

    CompletionItem(
      label = completionInfo.name,
      kind = symKind,
      detail = completionInfo.typeInfo.map(_.fullName)
    )
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
