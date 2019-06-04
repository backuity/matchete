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

import org.junit.Test


class TraversableMatchersTest extends JunitMatchers {

  import TestUtil.Person

  @Test
  def forAll() {
    List(1, 2, 3, 4) must forAll(be_<(10))
    Iterator(1, 2, 3) must forAll(be_<(10))
    List(2, 4, 6, 8) must forAll(be_<(10) and beLike("an even number") { case n if n % 2 == 0 => })
    Set(2, 4, 6, 8) must forAll(be_<(10) and beLike("an even number") { case n if n % 2 == 0 => })

    List[Int]() must forAll(be_<(10))

    {
      List(1, 2, 30, 40) must forAll(be_<(10))
    } must throwAn[AssertionError].withMessage(
      "List(1, 2, 30, 40) is not valid: 30 is not < 10")

    {
      List(1, 2, 3, 4) must forAll(beLike("< 3") { case i => require(i < 3, s"$i is not < 3") })
    } must throwAn[AssertionError].withMessage(
      "List(1, 2, 3, 4) is not valid: 3 is not like < 3: requirement failed: 3 is not < 3")
  }

  @Test
  def contain() {
    List(1, 2, 3) must contain(be_<(2))
    List(1, 2, 3) must contain(be_<(3))
    Iterator(1, 2, 3) must contain(be_<(3))
    List(1, 2, 3) must contain(be_<(2), be_<(2))

    {
      List[Int]() must contain(be_<(2))
    } must throwAn[AssertionError].withMessage(
      "List() does not contain be < 2")

    {
      List(Person("john", 15), Person("josephine", 12), Person("joseph", 7)) must contain(
        an("adult") { case Person(_, age) => age must be_>=(18) },
        an("'m' starting name") { case Person(name, _) => name must startWith("m") },
        a("'jo' starting name") { case Person(name, _) => name must startWith("jo") })
    } must throwAn[AssertionError].withMessage(
      """List(Person(john,15), Person(josephine,12), Person(joseph,7)) does not contain:
        |- an adult :
        |  * Person(john,15) is not an adult: 15 is not >= 18
        |  * Person(josephine,12) is not an adult: 12 is not >= 18
        |  * Person(joseph,7) is not an adult: 7 is not >= 18
        |- an 'm' starting name :
        |  * Person(john,15) is not an 'm' starting name: 'john' does not start with 'm'
        |  * Person(josephine,12) is not an 'm' starting name: 'josephine' does not start with 'm'
        |  * Person(joseph,7) is not an 'm' starting name: 'joseph' does not start with 'm'""".stripMargin)

    List[Int]() must not(contain(be_<(5), be_>(10)))
    List(8) must not(contain(be_<(5), be_>(10)))

    {
      List(1, 2, 3) must not(contain(an("even number") {
        case n => n % 2 must beEqualTo(1)
      }))
    } must throwAn[AssertionError].withMessage(
      "List(1, 2, 3) should not contain an even number")

    {
      List(2, 3) must not(contain(be_<(5)))
    } must throwAn[AssertionError].withMessage(
      "List(2, 3) should not contain be < 5")
  }

  @Test
  def containShouldFormatLongLists(): Unit = {
    {
      List(
        Person("this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name", 1234567890),
        Person("this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name too", 1234567890)
      ) must containExactly(a("person with name toto") {
        case Person(name, age) => name must_== "toto"
      })
    } must throwAn[AssertionError].withMessage(
      """List(
        |  Person(this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name,1234567890),
        |  Person(this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name too,1234567890))
        |
        |has size 2 but expected size 1 -- unexpected elements:
        |
        |- Person(this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name,1234567890) is not a person with name toto:
        |  'this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name'
        |
        |is not equal to
        |
        |  'toto'
        | expected:<t[oto]> but was:<t[his is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name]>
        |- Person(this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name too,1234567890) is not a person with name toto:
        |  'this is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name too'
        |
        |is not equal to
        |
        |  'toto'
        | expected:<t[ot]o> but was:<t[his is a looooooooooooooooooooooooooooooooooooooooooooooooo00000000000000000000000000000000ng name to]o>""".stripMargin)
  }

  @Test
  def containAny() {
    List(1, 2, 3) must containAny(be_<(5), be_>(100), be_>(200))
    Iterator(1, 2, 3) must containAny(be_<(5), be_>(100), be_>(200))
    List(1, 2, 3) must containAny(be_>(100), be_>(200), be_<(5))
    List(1) must containAny(be_>(5), be_==(1))
    List(1, 2, 3) must containAny(be_==(2))

    {
      List(1, 2, 3) must containAny(be_>(10), be_>(20))
    } must throwAn[AssertionError].withMessage(
      """List(1, 2, 3) does not contain any of:
        |- be > 10 :
        |  * 1 is not > 10
        |  * 2 is not > 10
        |  * 3 is not > 10
        |- be > 20 :
        |  * 1 is not > 20
        |  * 2 is not > 20
        |  * 3 is not > 20""".stripMargin)
  }

  @Test
  def containExactly() {
    List(1, 2) must containExactly(be_<(2), be_<(3))
    Iterator(1, 2, 3) must containExactly(be_<(10), be_<(20), be_<(30))
    Set(1, 2) must containExactly(be_<(2), be_<(3))
    Set(1, 2) must containExactly(be_==(1), be_==(2))
  }

  @Test
  def containExactly_UnsatisfiedElements() {

    // 1. not enough matchers
    // 1.a) all elements are matched
    {
      List(1, 2) must containExactly(be_<(3))
    } must throwAn[AssertionError].withMessage(
      "List(1, 2) has size 2 but expected size 1")

    // 1.b) no matchers
    {
      List(1, 2) must containExactly[Int]()
    } must throwAn[AssertionError].withMessage(
      "List(1, 2) has size 2 but expected size 0")

    // 1.c) some elements are unmatched
    {
      List(1, 2, 3) must containExactly(be_<(3))
    } must throwAn[AssertionError].withMessage(
      "List(1, 2, 3) has size 3 but expected size 1 -- unexpected elements 3 is not < 3")

    // 2. sizes match, but some elements are unmatched
    {
      List(Person("john", 28), Person("sophie", 12), Person("andrea", 17)) must containExactly(
        an("adult") { case Person(_, age) => age must be_>=(18) },
        a("4 letter name") { case Person(name, _) => name must haveSize(4) },
        a("'jo' starting name") { case Person(name, _) => name must startWith("jo") })
    } must throwAn[AssertionError].withMessage(
      """List(Person(john,28), Person(sophie,12), Person(andrea,17)) has unexpected elements:
        |- Person(sophie,12) :
        |  * is not an adult: 12 is not >= 18
        |  * is not a 4 letter name: 'sophie' has size 6 but expected size 4
        |  * is not a 'jo' starting name: 'sophie' does not start with 'jo'
        |- Person(andrea,17) :
        |  * is not an adult: 17 is not >= 18
        |  * is not a 4 letter name: 'andrea' has size 6 but expected size 4
        |  * is not a 'jo' starting name: 'andrea' does not start with 'jo'""".stripMargin)
  }

  @Test
  def containExactly_UnsatisfiedMatchers() {

    // 1.a) not enough elements
    {
      List(1) must containExactly(be_<(1), be_<(2), be_<(3), be_<(4))
    } must throwAn[AssertionError].withMessage(
      "List(1) has size 1 but expected size 4 -- does not contain be < 1 : 1 is not < 1")

    // 1.b) no elements
    {
      List[Int]() must containExactly(be_<(3))
    } must throwAn[AssertionError].withMessage(
      "List() has size 0 but expected size 1 -- does not contain be < 3")


    // 2. sizes match, but some matchers are unsatisfied
    {
      List(Person("john", 15), Person("josephine", 12), Person("joseph", 7)) must containExactly(
        an("adult") { case Person(_, age) => age must be_>=(18) },
        an("'m' starting name") { case Person(name, _) => name must startWith("m") },
        a("'jo' starting name") { case Person(name, _) => name must startWith("jo") })
    } must throwAn[AssertionError].withMessage(
      """List(Person(john,15), Person(josephine,12), Person(joseph,7)) does not contain:
        |- an adult :
        |  * Person(john,15) is not an adult: 15 is not >= 18
        |  * Person(josephine,12) is not an adult: 12 is not >= 18
        |  * Person(joseph,7) is not an adult: 7 is not >= 18
        |- an 'm' starting name :
        |  * Person(john,15) is not an 'm' starting name: 'john' does not start with 'm'
        |  * Person(josephine,12) is not an 'm' starting name: 'josephine' does not start with 'm'
        |  * Person(joseph,7) is not an 'm' starting name: 'joseph' does not start with 'm'""".stripMargin)
  }

  @Test
  def containExactly_InAnyOrder() {
    List(1, 2) must containExactly(be_<(3), be_<(2))
    Set(1, 2) must containExactly(be_<(3), be_<(2))

    // both elements satisfy both matchers
    List(Person("john", 15), Person("josephine", 12)) must containExactly(
      beLike("a kid") { case Person(_, age) => age must be_<(18) },
      beLike("a 'jo' starting name") { case Person(name, _) => name must startWith("jo") })

    // example from scaladoc
    List(1, 2, 4) must containExactly(be_<=(2), be_>(3), be_>=(4))
  }
}
