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

import org.junit.ComparisonFailure

trait JunitFailureReporter extends AssertionFailureReporter {
  override def failIfDifferentStrings(actual: String, expected: String, msg: String) {
    if( actual != expected ) throw new ComparisonFailure(msg, expected, actual)
  }
}

/**
 * Matchers that throw [[java.lang.AssertionError]] and [[org.junit.ComparisonFailure]] upon failure.
 */
trait JunitMatchers extends Matchers with JunitFailureReporter with ToMatcherOps
object junitMatchers1 extends JunitMatchers
