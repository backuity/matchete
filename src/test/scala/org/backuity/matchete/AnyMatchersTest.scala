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

  import AnyMatchersTest._

  @Test
  def beEqual() {
    List(1,2) must_== List(1,2)
    Person("john", 12) must_== Person("john", 12)
    123 must_== 123
    12.3 must_== 12.3

    Set(Person("John", 12), Person("Mary", 24)) must beEqualTo(Set(Person("Mary",24), Person("John", 12)))
    "john" must_== "john"

    {Person("john", 12) must beEqualTo(Person("mary", 21))} must throwAn[AssertionError].withMessage(
      "Person(john,12) is not equal to Person(mary,21)\nage = 12 ≠ age = 21")

    {123 must_== 321} must throwAn[AssertionError].withMessage(
      "123 is not equal to 321")
  }

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
  def beEqualStringShouldThrowComparisonFailure() {
    {"john" must_== "mary"} must throwA[ComparisonFailure].suchAs {
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
  def beEqual_Seq() {
    List(1,2,3) must_== List(1,2,3)
    Seq(1,2,3) must_== List(1,2,3)

    {Seq(1,2,3) must_== List(1,3,2)} must throwAn[AssertionError].withMessage(
      "List(1, 2, 3) is not equal to List(1, 3, 2), at index 2 expected 3 but got 2")

    {Seq(1,2,3) must_== List(1,2,3,4)} must throwAn[AssertionError].withMessage(
      "List(1, 2, 3) is not equal to List(1, 2, 3, 4), at index 4 expected 4 but got no element")

    {Seq(1,2,3) must_== List(1)} must throwAn[AssertionError].withMessage(
      "List(1, 2, 3) is not equal to List(1), at index 2 expected no element but got 2")

    {Seq.empty[Int] must_== List(1,2,3,4)} must throwAn[AssertionError].withMessage(
      "List() is not equal to List(1, 2, 3, 4), at index 1 expected 1 but got no element")
  }

  @Test
  def beEqual_Set() {
    Set(1,2,3) must_== Set(1,2,3)
    Set(1,2,3) must_== Set(3,2,1)

    {Set(1,2,3) must_== Set(1,2)} must throwAn[AssertionError].withMessage(
      "Set(1, 2, 3) is not equal to Set(1, 2), element 3 was not expected")

    {Set(2,3) must_== Set(1,2,3)} must throwAn[AssertionError].withMessage(
      "Set(2, 3) is not equal to Set(1, 2, 3), element 1 is missing")

    {Set(2,3) must_== Set(1,2,3,4)} must throwAn[AssertionError].withMessage(
      "Set(2, 3) is not equal to Set(1, 2, 3, 4), elements 1, 4 are missing")

    {Set(2,3,5) must_== Set(1,2,3,4)} must throwAn[AssertionError].withMessage(
      "Set(2, 3, 5) is not equal to Set(1, 2, 3, 4), elements 1, 4 are missing and element 5 was not expected")
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

    failure() must not(throwAn[IllegalStateException])

    { failure() must not(throwAn[Exception]) } must throwAn[AssertionError].withMessage(
      "java.lang.RuntimeException: baam should not throw an java.lang.Exception")
  }

  @Test
  def notShouldNotSwallowExpecteeExceptions() {
    def failure() : Int = throw new RuntimeException("baam")

    { failure() must not(be_<(12)) } must throwA[RuntimeException].withMessage("baam")
  }


  @Test
  def beA() {
    List(1,2) must beA[List[Int]]
    List(1,2) must beA[List[_]]
    new A1 must beA[A]

    {new A1 must beA[A2]} must throwAn[AssertionError].withMessage(
      "A1() is not a org.backuity.matchete.AnyMatchersTest.A2 it is a org.backuity.matchete.AnyMatchersTest.A1")

    {null.asInstanceOf[A2] must beA[A2]} must throwAn[AssertionError].withMessage(
      "null is not a org.backuity.matchete.AnyMatchersTest.A2")
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

    {List(1) must beEq(List(1))} must throwAn[AssertionError].like("not eq") {
      case e => e.getMessage must contain ("List(1)")
    }
  }

  @Test
  def beNull() {
    var a = "hello"
    a must not(beNull[String])

    {a must beNull[String]} must throwAn[AssertionError].withMessage(
      "'hello' is not null")

    a = null
    a must beNull[String]

    {a must not(beNull[String])} must throwAn[AssertionError].withMessage(
      "null should not be null")
  }
}

object AnyMatchersTest {
  case class Person(name: String, age: Int)
  
  trait Stuff {
    def name: String
    def price: Int
  }
  case class Flower(name: String, price: Int) extends Stuff
  case class Bike(name: String, price: Int, brand: String) extends Stuff
  case class Bucket(stuffs: List[Stuff])

  class A
  case class A1() extends A
  case class A2() extends A

  class ASizedClass {
    def size = 12
  }
}
