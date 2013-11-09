package org.backuity.matchers

import org.junit.{ComparisonFailure, Test}

class MatcherTest extends JunitMatchers {

  def typeSafeEqual() {
    illTyped("""
        List(1,2) must_== Set(1,2)
    """)

    illTyped("""
        List(1,2) must_!= Set(1,2)
    """)
  }

  @Test
  def and() {
    10 must (be_<(100) and be_>(1))

    {5 must (be_<(100) and be_>(10))} must throwAn[AssertionError].withMessage(
      "5 is not > 10")
  }

  @Test
  def or() {
    10 must (be_>(100) or be_<(20))

    {5 must (be_>(10) or be_<(1))} must throwAn[AssertionError].withMessage(
      "5 is not > 10 and 5 is not < 1")
  }

  @Test
  def customFormatter() {
    import AnyMatchersTest.Person

    implicit val customPersonFormatter = Formatter[Person]{ _.name }

    {Person("john", 12) must_== Person("mary", 24)} must throwAn[AssertionError].withMessage(
      "john is not equal to mary")

    {List(Person("john", 12)) must_== List(Person("mary", 24))} must throwAn[AssertionError].withMessage(
      "List(john) is not equal to List(mary)")
  }
}
