package org.github.dragos.vscode

import scala.util.Properties
import scala.reflect.io.Path
import org.ensime.api.ArchiveFile
import org.scalatest.FunSuite
import org.scalactic.TrySugar.Tryizer
import org.scalactic.TrySugar
import java.io.File

class TempFileStoreSuite extends FunSuite {

  test("Extraction works") {
    val tempStore = new TempFileStore("/tmp/testStore")

    // we don't know if we are run inside the root project, or the sub-project
    val jar = Path("src") / "test" / "resources" / "test.jar"
    val jar1 = if (jar.exists) jar else Path("ensime-lsp") / jar

    val extracted = tempStore.getFile(ArchiveFile(jar1.toAbsolute.jfile.toPath(), "/scala/Predef.scala"))
    assert(extracted.isSuccess, s"Couldn't extract archive $extracted")
  }
}
