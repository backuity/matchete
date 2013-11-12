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

class NumericMatchersTest extends JunitMatchers {

  @Test
  def testBeCloseTo() {
    12 must beCloseTo( 10 +/- 2 )
    12 must beCloseTo( 10 +/- 3 )
    12 must beCloseTo( 14 +/- 2 )
    12 must beCloseTo( 14 +/- 3 )

    -10 must beCloseTo( -8 +/- 3 )
    2 must beCloseTo( -1 +/- 3 )

    (12 must beCloseTo(10 +/- 1)) must throwAn[AssertionError].withMessage(
      "12 is not close to 10 +/- 1 (12 is > 11)")

    (-3 must beCloseTo(3 +/- 2)) must throwAn[AssertionError].withMessage(
      "-3 is not close to 3 +/- 2 (-3 is < 1)")
  }
}
