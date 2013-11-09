package org.backuity.matchers

import org.junit.Test

class StringMatchersTest extends JunitMatchers {
  @Test
  def startWith() {
    "john" must startWith("jo")

    {"mary" must startWith("jo")} must throwA[AssertionError].withMessage(
      "'mary' does not start with 'jo'")
  }

  @Test
  def contain() {
    "john is tired" must contain("is")
    "john is tired" must contain("ti")
    "john is tired" must contain("tired")
    "john is tired" must contain("john")

    {"john is tired" must contain("ist")} must throwAn[AssertionError].withMessage(
      "'john is tired' does not contain 'ist'")
  }
}
