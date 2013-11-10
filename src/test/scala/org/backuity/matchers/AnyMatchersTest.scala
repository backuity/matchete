package org.backuity.matchers

import org.junit.{ComparisonFailure, Test}

class AnyMatchersTest extends JunitMatchers {

  import AnyMatchersTest._

  @Test
  def beEqual() {
    List(1,2) must_== List(1,2)
    Set(Person("John", 12), Person("Mary", 24)) must beEqualTo(Set(Person("Mary",24), Person("John", 12)))
    "john" must_== "john"

    {List(1,2) must beEqualTo(List(1,2,3))} must throwAn[AssertionError].withMessage(
      "List(1, 2) is not equal to List(1, 2, 3)")

    {List(1,2) must_== List(1,2,3)} must throwAn[AssertionError].withMessage(
      "List(1, 2) is not equal to List(1, 2, 3)")
  }

  @Test
  def beEqualStringShouldThrowComparisonFailure() {
    {"john" must_== "mary"} must throwAn[ComparisonFailure].like("a comparison failure") {
      case c : ComparisonFailure =>
        c.getActual must_== "john"
        c.getExpected must_== "mary"
    }
  }

  @Test
  def beEqual_Array() {
    Array(1,2,3) must_== Array(1,2,3)

    {Array(1,2,3,4) must_== Array(1,2,4,3)} must throwAn[AssertionError].withMessage(
      "Array(1, 2, 3, 4) is not equal to Array(1, 2, 4, 3)")
  }

  @Test
  def beNotEqual() {
    List(1,2) must_!= List(1,2,3)

    {List(1,2) must_!= List(1,2)} must throwAn[AssertionError].withMessage(
      "List(1, 2) should not be equal to List(1, 2)")
  }

  @Test
  def beNotEqual_Array() {
    Array(1,2,3) must_!= Array(1,2)

    {Array(1,2) must_!= Array(1,2)} must throwAn[AssertionError].withMessage(
      "Array(1, 2) should not be equal to Array(1, 2)")
  }

  @Test
  def isEmpty() {
    List() must beEmpty
    List(1,2,3) must not(beEmpty)
    "" must beEmpty
    "hello" must not(beEmpty)

    {List(1,2,3) must beEmpty} must throwAn[AssertionError].withMessage(
      "List(1, 2, 3) is not empty")

    {List() must not(beEmpty)} must throwAn[AssertionError].withMessage(
      "List() should not be empty")
  }

  @Test
  def haveSize() {
    "" must haveSize(0)
    "sophie" must haveSize(6)
    Array(1,2) must haveSize(2)

    List(1,2,3) must haveSize(3)
    Set("a", "b", "c", "d") must haveSize(4)
    Set.empty[Int] must haveSize(0)
    new ASizedClass must haveSize(12)

    {List(1,2,3) must haveSize(12)} must throwAn[AssertionError].withMessage(
      "List(1, 2, 3) has size 3 but expected size 12")

    {Set(1,2,3) must haveSize(2)} must throwAn[AssertionError].withMessage(
      "Set(1, 2, 3) has size 3 but expected size 2")

    {"robert" must haveSize(5)} must throwAn[AssertionError].withMessage(
      "'robert' has size 6 but expected size 5")

    {Array(1,2,3) must haveSize(2)} must throwAn[AssertionError].withMessage(
      "Array(1, 2, 3) has size 3 but expected size 2")
  }

  @Test
  def not() {
    List(1,2) must not(haveSize(0))
    List(1,2) must not(beEmpty)

    {List() must not(haveSize(0))} must throwAn[AssertionError].withMessage(
      "List() should not have size 0")

    {List() must not(beEmpty)} must throwAn[AssertionError].withMessage(
      "List() should not be empty")

    {"john" must not(be(a("name with vowels"){ case n => n must contain("o")}))} must throwAn[AssertionError].withMessage(
      "'john' should not be a name with vowels")

    def failure() { throw new RuntimeException("baam") }

    { failure() must not(throwAn[Exception]) } must throwAn[AssertionError].withMessage(
      "java.lang.RuntimeException: baam should not throw an java.lang.Exception")
  }


  @Test
  def beA() {
    List(1,2) must beA[List[Int]]
    List(1,2) must beA[List[_]]
    new A1 must beA[A]

    {new A1 must beA[A2]} must throwAn[AssertionError].withMessage(
      "A1() is not a org.backuity.matchers.AnyMatchersTest.A2 it is a org.backuity.matchers.AnyMatchersTest.A1")
  }
}

object AnyMatchersTest {
  case class Person(name: String, age: Int)

  class A
  case class A1() extends A
  case class A2() extends A

  class ASizedClass {
    def size = 12
  }
}
