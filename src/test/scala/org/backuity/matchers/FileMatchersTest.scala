package org.backuity.matchers

import org.junit.Test
import java.io.File

class FileMatchersTest extends JunitMatchers with FileMatchers {

  @Test
  def testExist() {
    new File(".") must exist

    (new File("/that/doesnt/exist") must exist) must throwAn[AssertionError].withMessage(
      "/that/doesnt/exist do not exist")
  }
}
