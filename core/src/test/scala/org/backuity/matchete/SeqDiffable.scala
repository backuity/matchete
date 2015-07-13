package org.backuity.matchete

import org.backuity.matchete.TestUtil._
import org.junit.{ComparisonFailure, Test}

class SeqDiffable extends JunitMatchers {

  @Test
  def beEqualNestedList_Ok(): Unit = {
    implicit val stuffDiffable : Diffable[Stuff] = Diffable.forFields(_.name)
    Bucket(List(Flower("john", 12))) must_== Bucket(List(Bike("john", 21, "BMX")))
  }

  @Test
  def beEqualNestedList_Error(): Unit = {
    implicit val stuffDiffable : Diffable[Stuff] = Diffable.forFields(_.name, _.price)

    {Bucket(List(Flower("john", 12))) must_== Bucket(List(Bike("john", 21, "BMX")))} must throwAn[AssertionError].withMessage(
      """Bucket(List(Flower(john,12))) is not equal to Bucket(List(Bike(john,21,BMX)))
        |stuffs.(0).price = 12 ≠ stuffs.(0).price = 21""".stripMargin)
  }

  @Test
  def beEqualNestedList_Error_DifferentSize(): Unit = {

    {Bucket(List(Flower("john", 12), Flower("dude",12))) must_== Bucket(List(Bike("john", 21, "BMX")))} must throwAn[AssertionError].withMessage(
      """Bucket(List(Flower(john,12), Flower(dude,12))) is not equal to Bucket(List(Bike(john,21,BMX)))
        |stuffs.size = 2 ≠ stuffs.size = 1""".stripMargin)
  }

  @Test
  def beEqualNestedList_ShouldThrowComparisonFailureForStringFields(): Unit = {
    implicit val stuffDiffable : Diffable[Stuff] = Diffable.forFields(_.name)

    {Bucket(List(Flower("x",12),Flower("john toto", 12))) must_== Bucket(List(Bike("x",13,"y"),Bike("john X toto", 21, "BMX")))} must throwA[ComparisonFailure].suchAs {
      case c : ComparisonFailure =>
        c.getMessage must_== """Bucket(List(Flower(x,12), Flower(john toto,12))) is not equal to Bucket(List(Bike(x,13,y), Bike(john X toto,21,BMX)))
                               |stuffs.(1).name = john toto ≠ stuffs.(1).name = john X toto expected:<john [X ]toto> but was:<john []toto>""".stripMargin
        c.getActual must_== "john toto"
        c.getExpected must_== "john X toto"
    }
  }

  @Test
  def beEqual_Seq() {
    List(1,2,3) must_== List(1,2,3)
    Seq(1,2,3) must_== List(1,2,3)

    {List(Person("John", 21), Person("Jane", 32)) must_== List(Person("John", 21), Person("Jane", 12))} must throwAn[AssertionError].withMessage(
      """List(Person(John,21), Person(Jane,32)) is not equal to List(Person(John,21), Person(Jane,12))
        |(1).age = 32 ≠ (1).age = 12""".stripMargin)

    {Seq(1,2,3) must_== List(1,3,2)} must throwAn[AssertionError].withMessage(
      """List(1, 2, 3) is not equal to List(1, 3, 2)
        |(1) = 2 ≠ (1) = 3""".stripMargin)

    {Seq(1,2,3) must_== List(1,2,3,4)} must throwAn[AssertionError].withMessage(
      """List(1, 2, 3) is not equal to List(1, 2, 3, 4)
        |size = 3 ≠ size = 4""".stripMargin)

    {Seq(1,2,3) must_== List(1)} must throwAn[AssertionError].withMessage(
      """List(1, 2, 3) is not equal to List(1)
        |size = 3 ≠ size = 1""".stripMargin)

    {Seq.empty[Int] must_== List(1,2,3,4)} must throwAn[AssertionError].withMessage(
      """List() is not equal to List(1, 2, 3, 4)
        |size = 0 ≠ size = 4""".stripMargin)
  }
}
