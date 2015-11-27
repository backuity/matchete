/*
 * Copyright 2013 Bruno Bieth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.backuity.matchete

import org.junit.{ComparisonFailure, Test}

class AnyMatchersTest extends JunitMatchers {

  import TestUtil._

  @Test
  def beEqual() {
    List(1, 2) must_== List(1, 2)
    Person("john", 12) must_== Person("john", 12)
    123 must_== 123
    12.3 must_== 12.3

    Set(Person("John", 12), Person("Mary", 24)) must beEqualTo(Set(Person("Mary", 24), Person("John", 12)))
    "john" must_== "john"

    {
      Person("john", 12) must beEqualTo(Person("mary", 21))
    } must throwAn[AssertionError].withMessage(
      """Person(john,12) is not equal to Person(mary,21)
        |Got     : age = 12
        |Expected: age = 21""".stripMargin)

    {
      123 must_== 321
    } must throwAn[AssertionError].withMessage(
      "123 is not equal to 321")
  }

  @Test
  def beEqualStringShouldThrowComparisonFailure() {
    {
      "john" must_== "mary"
    } must throwA[ComparisonFailure].suchAs {
      case c: ComparisonFailure =>
        c.getActual must_== "john"
        c.getExpected must_== "mary"
    }
  }

  @Test
  def beEqual_Array() {
    Array(1, 2, 3) must_== Array(1, 2, 3)

    {
      Array(1, 2, 3, 4) must_== Array(1, 2, 4, 3)
    } must throwAn[AssertionError].withMessage(
      "Array(1, 2, 3, 4) is not equal to Array(1, 2, 4, 3)")
  }

  @Test
  def beNotEqual() {
    List(1, 2) must_!= List(1, 2, 3)

    {
      List(1, 2) must_!= List(1, 2)
    } must throwAn[AssertionError].withMessage(
      "List(1, 2) should not be equal to List(1, 2)")
  }

  @Test
  def beNotEqual_Array() {
    Array(1, 2, 3) must_!= Array(1, 2)

    {
      Array(1, 2) must_!= Array(1, 2)
    } must throwAn[AssertionError].withMessage(
      "Array(1, 2) should not be equal to Array(1, 2)")
  }

  @Test
  def isEmpty() {
    List() must beEmpty
    List(1, 2, 3) must not(beEmpty)
    "" must beEmpty
    "hello" must not(beEmpty)

    {
      List(1, 2, 3) must beEmpty
    } must throwAn[AssertionError].withMessage(
      "List(1, 2, 3) is not empty")

    {
      List() must not(beEmpty)
    } must throwAn[AssertionError].withMessage(
      "List() should not be empty")
  }

  @Test
  def anEmpty(): Unit = {
    List(Group(Nil)) must containExactly(anEmpty[Group])

    {
      List() must containExactly(anEmpty[Group])
    } must throwAn[AssertionError].withMessage(
      "List() has size 0 but expected size 1 -- does not contain an empty Group")
  }

  @Test
  def haveSize() {
    "" must haveSize(0)
    "sophie" must haveSize(6)
    Array(1, 2) must haveSize(2)

    List(1, 2, 3) must haveSize(3)
    Set("a", "b", "c", "d") must haveSize(4)
    Set.empty[Int] must haveSize(0)
    new ASizedClass must haveSize(12)

    {
      List(1, 2, 3) must haveSize(12)
    } must throwAn[AssertionError].withMessage(
      "List(1, 2, 3) has size 3 but expected size 12")

    {
      Set(1, 2, 3) must haveSize(2)
    } must throwAn[AssertionError].withMessage(
      "Set(1, 2, 3) has size 3 but expected size 2")

    {
      "robert" must haveSize(5)
    } must throwAn[AssertionError].withMessage(
      "'robert' has size 6 but expected size 5")

    {
      Array(1, 2, 3) must haveSize(2)
    } must throwAn[AssertionError].withMessage(
      "Array(1, 2, 3) has size 3 but expected size 2")
  }

  @Test
  def not() {
    List(1, 2) must not(haveSize(0))
    List(1, 2) must not(beEmpty)

    {
      List() must not(haveSize(0))
    } must throwAn[AssertionError].withMessage(
      "List() should not have size 0")

    {
      List() must not(beEmpty)
    } must throwAn[AssertionError].withMessage(
      "List() should not be empty")

    {
      "john" must not(be(a("name with vowels") { case n => n must contain("o") }))
    } must throwAn[AssertionError].withMessage(
      "'john' should not be a name with vowels")

    def failure() {
      throw new RuntimeException("baam")
    }

    failure() must not(throwAn[IllegalStateException])

    {
      failure() must not(throwAn[Exception])
    } must throwAn[AssertionError].withMessage(
      "java.lang.RuntimeException: baam should not throw an java.lang.Exception")
  }

  @Test
  def notShouldNotSwallowExpecteeExceptions() {
    def failure(): Int = throw new RuntimeException("baam")

    {
      failure() must not(be_<(12))
    } must throwA[RuntimeException].withMessage("baam")
  }

  @Test
  def beA() {
    List(1, 2) must beA[List[Int]]
    List(1, 2) must beA[List[_]]
    new A1 must beA[A]

    {
      new A1 must beA[A2]
    } must throwAn[AssertionError].withMessage(
      "A1() is not a org.backuity.matchete.TestUtil.A2 it is a org.backuity.matchete.TestUtil.A1")

    {
      null.asInstanceOf[A2] must beA[A2]
    } must throwAn[AssertionError].withMessage(
      "null is not a org.backuity.matchete.TestUtil.A2")
  }

  @Test
  def beEq() {
    val a1 = new A
    val a2 = a1
    a1 must beEq(a1)
    a1 must beEq(a2)
    a2 must beEq(a1)

    new A must not(beEq(new A))
    List(1) must not(beEq(List(1)))

    {
      List(1) must beEq(List(1))
    } must throwAn[AssertionError].like("not eq") {
      case e => e.getMessage must contain("List(1)")
    }
  }

  @Test
  def beNull() {
    var a = "hello"
    a must not(beNull[String])

    {
      a must beNull[String]
    } must throwAn[AssertionError].withMessage(
      "'hello' is not null")

    a = null
    a must beNull[String]

    {
      a must not(beNull[String])
    } must throwAn[AssertionError].withMessage(
      "null should not be null")
  }
}

