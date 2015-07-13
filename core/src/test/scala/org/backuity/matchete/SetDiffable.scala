package org.backuity.matchete

import org.junit.Test

class SetDiffable extends JunitMatchers {
  import TestUtil._

  @Test
  def sameSetDiff(): Unit = {
    Set(1,2,3) must_== Set(3,2,1)
    Set(Person("john", 12), Person("doe", 23)) must_== Set(Person("john", 12), Person("doe", 23))
  }

  @Test
  def diffMustShowMissingElements(): Unit = {
    implicit val personFormatter : Formatter[Person] = Formatter[Person] { person => s"${person.name}(${person.age})" }

    {Set(1, 2, 3) must_== Set(1, 2, 4)} must throwAn[AssertionError].withMessage(
      """Set(1, 2, 3) is not equal to Set(1, 2, 4):
        | * extra elements: 3
        | * missing elements: 4""".stripMargin
    )

    {Set(Person("john", 12), Person("doe", 23)) must_== Set(Person("doe", 99), Person("john", 12))} must throwAn[AssertionError].withMessage(
      """Set(john(12), doe(23)) is not equal to Set(doe(99), john(12)):
        | * extra elements: doe(23)
        | * missing elements: doe(99)""".stripMargin
    )
  }
}
