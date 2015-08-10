package org.backuity.matchete

import org.backuity.matchete.TestUtil._
import org.junit.{ComparisonFailure, Test}

class SeqDiffableTest extends JunitMatchers {

  @Test
  def beEqualNestedList_Ok(): Unit = {
    {List(Person("John", 21), Person("Jane", 32)) must_== List(Person("John", 21), Person("Jane", 12))} must throwAn[AssertionError].withMessage(
      """List(Person(John,21), Person(Jane,32)) is not equal to List(Person(John,21), Person(Jane,12))
        |Got     : (1).age = 32
        |Expected: (1).age = 12""".stripMargin)

    implicit val diffableNACC : Diffable[CustomEqual] = Diffable.forFields[CustomEqual](_.str, _.int)

    {List(new CustomEqual("one", 1),new CustomEqual("two", 2)) must_== List(new CustomEqual("one", 2), new CustomEqual("two", 2))} must throwAn[AssertionError].withMessage(
    """List(CE(one,1), CE(two,2)) is not equal to List(CE(one,2), CE(two,2))
      |Got     : (0).int = 1
      |Expected: (0).int = 2""".stripMargin)
  }

  @Test
  def beEqualNestedList_Error(): Unit = {
    implicit val stuffDiffable : Diffable[Stuff] = Diffable.forFields(_.name, _.price)

    {Bucket(List(Flower("john", 12))) must_== Bucket(List(Bike("john", 21, "BMX")))} must throwAn[AssertionError].withMessage(
      """Bucket(List(Flower(john,12))) is not equal to Bucket(List(Bike(john,21,BMX)))
        |Got     : stuffs.(0).price = 12
        |Expected: stuffs.(0).price = 21""".stripMargin)
  }

  @Test
  def beEqualNestedList_Error_DifferentSize(): Unit = {

    {Bucket(List(Flower("john", 12), Flower("dude",12))) must_== Bucket(List(Bike("john", 21, "BMX")))} must throwAn[AssertionError].withMessage(
      """Bucket(List(Flower(john,12), Flower(dude,12))) is not equal to Bucket(List(Bike(john,21,BMX)))
        |Got     : stuffs.size = 2
        |Expected: stuffs.size = 1""".stripMargin)
  }

  @Test
  def beEqualNestedList_ShouldThrowComparisonFailureForStringFields(): Unit = {
    implicit val stuffDiffable : Diffable[Stuff] = Diffable.forFields(_.name, _.price)

    {Bucket(List(Flower("x",13),Flower("john toto", 12))) must_== Bucket(List(Flower("x",13),Flower("john X toto", 21)))} must throwA[ComparisonFailure].suchAs {
      case c : ComparisonFailure =>
        c.getMessage must_==
          """
            |  Bucket(List(Flower(x,13), Flower(john toto,12)))
            |
            |is not equal to
            |
            |  Bucket(List(Flower(x,13), Flower(john X toto,21)))
            |
            |Got     : stuffs.(1).name = 'john toto'
            |Expected: stuffs.(1).name = 'john X toto' expected:<john [X ]toto> but was:<john []toto>""".stripMargin
        c.getActual must_== "john toto"
        c.getExpected must_== "john X toto"
    }
  }

  @Test
  def beEqual_Seq() {
    List(1,2,3) must_== List(1,2,3)
    Seq(1,2,3) must_== List(1,2,3)

    {Seq(1,2,3) must_== List(1,3,2)} must throwAn[AssertionError].withMessage(
      """List(1, 2, 3) is not equal to List(1, 3, 2)
        |Got     : (1) = 2
        |Expected: (1) = 3""".stripMargin)

    {Seq(1,2,3) must_== List(1,2,3,4)} must throwAn[AssertionError].withMessage(
      """List(1, 2, 3) is not equal to List(1, 2, 3, 4)
        |Got     : size = 3
        |Expected: size = 4""".stripMargin)

    {Seq(1,2,3) must_== List(1)} must throwAn[AssertionError].withMessage(
      """List(1, 2, 3) is not equal to List(1)
        |Got     : size = 3
        |Expected: size = 1""".stripMargin)

    {Seq.empty[Int] must_== List(1,2,3,4)} must throwAn[AssertionError].withMessage(
      """List() is not equal to List(1, 2, 3, 4)
        |Got     : size = 0
        |Expected: size = 4""".stripMargin)
  }
}
