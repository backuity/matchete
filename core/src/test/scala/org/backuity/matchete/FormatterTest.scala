package org.backuity.matchete

import org.junit.{Ignore, ComparisonFailure, Test}

class FormatterTest extends JunitMatchers {

  import TestUtil.Person

  @Test
  def customFormatter() {

    implicit val customPersonFormatter = Formatter[Person] {
      _.name
    }

    {
      Person("john", 12) must_== Person("john", 24)
    } must throwAn[AssertionError].withMessage(
      """john is not equal to john
        |Got     : age = 12
        |Expected: age = 24""".stripMargin)

    {
      List(Person("john", 12)) must_== List(Person("john", 24))
    } must throwAn[AssertionError].withMessage(
      """List(john) is not equal to List(john)
        |Got     : (0).age = 12
        |Expected: (0).age = 24""".stripMargin)
  }

  @Test
  def tupleFormatter(): Unit = {
    implicit val customPersonFormatter = Formatter[Person] {
      _.name
    }

    {
      (Person("john", 13), 12) must_==(Person("john", 12), 12)
    } must throwAn[AssertionError].withMessage(
      """(john,12) is not equal to (john,12)
        |Got     : _1.age = 13
        |Expected: _1.age = 12""".stripMargin)
  }

  @Test
  def longListFormatter(): Unit = {
    {
      List(
        Person("this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name", 1234567890),
        Person("this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name too", 1234567890)
      ) must_== List(Person("joe", 12))
    } must throwAn[AssertionError].withMessage(
      """
        |  List(
        |    Person(this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name,1234567890),
        |    Person(this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name too,1234567890))
        |
        |is not equal to
        |
        |  List(Person(joe,12))
        |
        |Got     : size = 2
        |Expected: size = 1""".stripMargin)
  }

  @Test
  def mapFormatterShouldSortMapIfPossible(): Unit = {
    // no ordering defined on Person, maps won't be sorted
    {
      Map(Person("mary", 13) -> 12, Person("john", 12) -> 12) must_==
        Map(Person("john", 12) -> 21, Person("mary", 13) -> 12)
    } must throwAn[AssertionError].withMessage(
      """
        |  Map(Person(mary,13) -> 12, Person(john,12) -> 12)
        |
        |is not equal to
        |
        |  Map(Person(john,12) -> 21, Person(mary,13) -> 12)
        |
        |Got     : get(Person(john,12)) = 12
        |Expected: get(Person(john,12)) = 21""".stripMargin
    )

    {
      Map(1 -> "a", 2 -> "b", 3 -> "c", 4 -> "d") must_==
        Map(2 -> "a", 1 -> "a", 4 -> "d", 3 -> "c")
    } must throwAn[ComparisonFailure].withMessage(
      """
        |  Map(1 -> 'a', 2 -> 'b', 3 -> 'c', 4 -> 'd')
        |
        |is not equal to
        |
        |  Map(1 -> 'a', 2 -> 'a', 3 -> 'c', 4 -> 'd')
        |
        |Got     : get(2) = 'b'
        |Expected: get(2) = 'a' expected:<[a]> but was:<[b]>""".stripMargin
    )
  }

  // TODO if Formatter implicits gets implemented through macros or if scala changes specificity wrt contravariance
  //      see the note in Formatter
  @Test
  @Ignore
  def setFormatterShouldSortSetIfPossible(): Unit = {
    {
      Set(1, 2, 8, 5, 4, 3) must_== Set(3, 4, 5, 1, 6, 2, 9)
    } must throwAn[AssertionError].withMessage(
      """Set(1, 2, 3, 4, 5, 8) is not equal to Set(1, 2, 3, 4, 5, 6, 9)
        |Reasons:
        | * extra elements: 8
        | * missing elements: 9, 6""".stripMargin
    )
  }

  @Test
  def longMapFormatter(): Unit = {
    {
      Map(
        12 -> Person("this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name", 1234567890),
        23 -> Person("john", 12)) must_==
        Map(
          23 -> Person("john", 21),
          12 -> Person("this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name", 1234567890))
    } must throwAn[AssertionError].withMessage(
      """
        |  Map(
        |    12 -> Person(this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name,1234567890),
        |    23 -> Person(john,12))
        |
        |is not equal to
        |
        |  Map(
        |    12 -> Person(this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name,1234567890),
        |    23 -> Person(john,21))
        |
        |Got     : get(23).age = 12
        |Expected: get(23).age = 21""".stripMargin)
  }

  @Test
  def diffLongElements(): Unit = {
    {
      List(Set(Person("this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name", 1234567890),
        Person("johnny", 23))) must_==
        List(Set(Person("johnny", 2345678),
          Person("this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name", 1234567890),
          Person("maryjane", 234),
          Person("toto", 1),
          Person("toto", 2),
          Person("toto", 3),
          Person("toto", 4),
          Person("toto", 5),
          Person("toto", 6),
          Person("toto", 7)))
    } must throwAn[AssertionError].withMessage(
      """
        |  List(
        |    Set(
        |      Person(this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name,1234567890),
        |      Person(johnny,23)))
        |
        |is not equal to
        |
        |  List(
        |    Set(
        |      Person(toto,6),
        |      Person(toto,4),
        |      Person(this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name,1234567890),
        |      Person(toto,5),
        |      Person(toto,3),
        |      Person(toto,2),
        |      Person(johnny,2345678),
        |      Person(toto,1),
        |      Person(maryjane,234),
        |      Person(toto,7)))
        |
        |Got     : (0) = Set(
        |                  Person(this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name,1234567890),
        |                  Person(johnny,23))
        |Expected: (0) = Set(
        |                  Person(toto,6),
        |                  Person(toto,4),
        |                  Person(this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name,1234567890),
        |                  Person(toto,5),
        |                  Person(toto,3),
        |                  Person(toto,2),
        |                  Person(johnny,2345678),
        |                  Person(toto,1),
        |                  Person(maryjane,234),
        |                  Person(toto,7))
        |Reasons:
        | * extra elements: Person(johnny,23)
        | * missing elements: Person(toto,6),
        |                     Person(toto,4),
        |                     Person(toto,5),
        |                     Person(toto,3),
        |                     Person(toto,2),
        |                     Person(johnny,2345678),
        |                     Person(toto,1),
        |                     Person(maryjane,234),
        |                     Person(toto,7)""".stripMargin
    )
  }
}
