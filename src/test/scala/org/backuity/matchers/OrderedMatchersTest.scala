package org.backuity.matchers

import org.junit.Test

class OrderedMatchersTest extends JunitMatchers {

  @Test
  def orderedMatchers() {
    "abc" must be_<("bcd")

    {"bcd" must be_<("abc")} must throwAn[AssertionError].withMessage(
      "'bcd' is not < 'abc'")
  }
}
