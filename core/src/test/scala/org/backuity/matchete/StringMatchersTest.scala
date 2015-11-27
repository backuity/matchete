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

class StringMatchersTest extends JunitMatchers {
  @Test
  def startWith() {
    "john" must startWith("jo")

    {
      "mary" must startWith("jo")
    } must throwA[AssertionError].withMessage(
      "'mary' does not start with 'jo'")
  }

  @Test
  def contain() {
    "john is tired" must contain("is")
    "john is tired" must contain("ti")
    "john is tired" must contain("tired")
    "john is tired" must contain("john")

    "\njohn\nis\ntired" must (contain("john") and contain("is") and contain("tired"))

    {
      "john is tired" must contain("ist")
    } must throwAn[AssertionError].withMessage(
      "'john is tired' does not contain 'ist'")
  }
}
