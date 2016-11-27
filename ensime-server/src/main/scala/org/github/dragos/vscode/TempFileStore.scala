package org.github.dragos.vscode

import java.net.URI
import java.nio.file._

import scala.util.Success
import scala.util.Try

import org.ensime.api._

import com.typesafe.scalalogging.LazyLogging

/**
 * A place to unzip files from archives
 */
class TempFileStore(path: String) extends LazyLogging {
  val rootPath = FileSystems.getDefault.getPath(path)

  if (!Files.exists(rootPath)) {
    Files.createDirectory(rootPath)
  }

  assert(Files.isDirectory(rootPath), s"File store $path is not a directory")

  def getFile(path: EnsimeFile): Try[Path] = path match {
    case RawFile(path) => Success(path)
    case ArchiveFile(jar, entry) => Try {
      logger.info(s"Extracting $jar!$entry to $rootPath")
      val uri = URI.create(s"jar:file:$jar")
      val zipFile = FileSystems.newFileSystem(uri, new java.util.HashMap[String, String])
      val zipFilePath = zipFile.getPath(entry)
      val targetPath = if (entry.startsWith("/")) entry.drop(1) else entry
      val extractedPath = rootPath.resolve(targetPath)

      try {
        Files.createDirectories(extractedPath.getParent)
        Files.copy(zipFilePath, extractedPath, StandardCopyOption.REPLACE_EXISTING)
      } finally zipFile.close()

      extractedPath
    }
  }
}
