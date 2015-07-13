package org.backuity.matchete

import org.junit.Test

class MapDiffableTest extends JunitMatchers {

  import TestUtil._

  @Test
  def diffMaps_sameKeysDifferentValues(): Unit = {
    val mapA = Map(
      12 -> Person("john",12),
      21 -> Person("mary",21))

    val mapB = Map(
      12 -> Person("john",12),
      21 -> Person("mary",13))

    {
      mapA must_== mapB
    } must throwAn[AssertionError].withMessage(
      """Map((12,Person(john,12)), (21,Person(mary,21))) is not equal to Map((12,Person(john,12)), (21,Person(mary,13)))
        |Got     : get(21).age = 21
        |Expected: get(21).age = 13""".stripMargin
    )
  }

  @Test
  def diffMaps_differentKeys(): Unit = {
    val mapA = Map(
      12 -> Person("john",12),
      21 -> Person("mary",21))

    val mapB = Map(
      12 -> Person("john",12))

    {
      mapA must_== mapB
    } must throwAn[AssertionError].withMessage(
      """Map((12,Person(john,12)), (21,Person(mary,21))) is not equal to Map((12,Person(john,12)))
        |Got     : get(21) = Some(Person(mary,21))
        |Expected: get(21) = None""".stripMargin
    )
  }
}
