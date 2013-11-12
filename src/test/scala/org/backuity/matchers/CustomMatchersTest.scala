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

trait AgeMatchers extends MatcherSupport {

  import CustomMatchersTest.Person

  def beAnAdult : Matcher[Int] = beLike("an adult") { case age => age must be_>=(18) }

  // type inference variation
  def beAnAdult2 = beLike[Int]("an adult") { case age => age must be_>=(18) }

  def beAnAdult3 = matcher[Person](
    description = "be an adult",
    validate = _.age >= 18,
    failureDescription = person => s"$person is not an adult, its age (${person.age}}) isn't >= 18"
  )
}

object CustomMatchersTest {
  case class Person(name: String, age: Int)
}

class CustomMatchersTest {

}
