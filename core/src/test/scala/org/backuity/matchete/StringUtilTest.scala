package org.backuity.matchete

import org.junit.Test

class StringUtilTest extends JunitMatchers {

  import StringUtil.diff

  @Test
  def diffDifferentStrings(): Unit = {
    diff("Add(file=stuff)", "Add(file=pouette)") must_== "Add(file=..u.t..)"
  }

  @Test
  def diffSameStrings(): Unit = {
    diff("Same", "Same") must_== "Same"
  }
}
