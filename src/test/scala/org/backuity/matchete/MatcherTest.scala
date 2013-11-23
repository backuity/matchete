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
  def matcherShouldBeContravariant() {
    Seq(1,2,3) must_== List(1,2,3)
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
      "List(john) is not equal to List(mary), at index 1 expected mary but got john")
  }
}
