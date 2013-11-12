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

package org.backuity.matchers

import org.junit.Test

class CoreMatcherSupportTest extends JunitMatchers {
  import CoreMatcherSupportTest.Person

  @Test
  def partialFunctionMatch() {
    Some(1) must beLike("< 10") { case Some(i) => require(i < 10, s"$i isn't < 10") }

    val johnIsNotAnAdult = "Person(john,15) is not an adult: 15 is not >= 18"

    {Person("john", 15) must be("an adult") { case Person(_,age) => age must be_>=(18)}} must throwAn[AssertionError].withMessage(
      johnIsNotAnAdult)

    {Person("john", 15) must be(an("adult") { case Person(_,age) => age must be_>=(18)})} must throwAn[AssertionError].withMessage(
      johnIsNotAnAdult)

    {Some(10) must beLike("< 10") { case Some(i) => require(i < 10, s"$i isn't < 10") }} must throwAn[AssertionError].withMessage(
      "Some(10) is not like < 10: requirement failed: 10 isn't < 10")
  }
}

object CoreMatcherSupportTest {
  case class Person(name: String, age: Int)
}
