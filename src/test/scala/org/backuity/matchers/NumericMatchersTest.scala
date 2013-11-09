package org.backuity.matchers

import org.junit.Test

class NumericMatchersTest extends JunitMatchers {

  @Test
  def testBeCloseTo() {
    12 must beCloseTo( 10 +/- 2 )
    12 must beCloseTo( 10 +/- 3 )
    12 must beCloseTo( 14 +/- 2 )
    12 must beCloseTo( 14 +/- 3 )

    -10 must beCloseTo( -8 +/- 3 )
    2 must beCloseTo( -1 +/- 3 )

    (12 must beCloseTo(10 +/- 1)) must throwAn[AssertionError].withMessage(
      "12 is not close to 10 +/- 1 (12 is > 11)")

    (-3 must beCloseTo(3 +/- 2)) must throwAn[AssertionError].withMessage(
      "-3 is not close to 3 +/- 2 (-3 is < 1)")
  }
}
