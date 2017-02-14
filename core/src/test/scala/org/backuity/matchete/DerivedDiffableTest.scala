package org.backuity.matchete

import org.backuity.matchete.TestUtil.{Bike, Flower, Person, Stuff}
import org.junit.Test

class DerivedDiffableTest extends JunitMatchers {

  @Test
  def eitherShouldBeDiffable(): Unit = {
    val either: Either[String, Person] = Right(Person("john", 12))

    {
      either must_== Right(Person("john", 13))
    } must throwAn[AssertionError].withMessage(
      """Right(Person(john,12)) is not equal to Right(Person(john,13))
        |Got     : value.age = 12
        |Expected: value.age = 13""".stripMargin
    )
  }

  @Test
  def sealedHierarchiesShouldBeDiffable(): Unit = {
    val stuff: Stuff = Flower("rosa", 12)

    stuff must_== Flower("rosa", 12)

    {
      stuff must_== Flower("rosa", 22)
    } must throwAn[AssertionError].withMessage(
      """Flower(rosa,12) is not equal to Flower(rosa,22)
        |Got     : price = 12
        |Expected: price = 22""".stripMargin
    )

    {
      stuff must_== Bike("bmx", 12, "xyz")
    } must throwAn[AssertionError].withMessage(
      """Flower(rosa,12) is not equal to Bike(bmx,12,xyz)
        |Got     : Flower(rosa,12)
        |Expected: Bike(bmx,12,xyz)""".stripMargin
    )
  }
}
