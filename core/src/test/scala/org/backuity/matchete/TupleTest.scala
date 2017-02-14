package org.backuity.matchete

import org.junit.{ComparisonFailure, Test}

class TupleTest extends JunitMatchers {

  @Test
  def compareTuple(): Unit = {
    (1, 2, 3) must_== (1, 2, 3)

    {
      (12, "john doe", true) must_== (12, "john doex", true)
    } must throwA[ComparisonFailure].withMessage(
      """(12,'john doe',true) is not equal to (12,'john doex',true)
        |Got     : _2 = 'john doe'
        |Expected: _2 = 'john doex' expected:<john doe[x]> but was:<john doe[]>""".stripMargin
    )
  }
}
