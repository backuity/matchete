package org.backuity.matchete

import org.junit.ComparisonFailure

// A bit of copy pasting in order to break a dependency on junit

trait JunitFailureReporter extends AssertionFailureReporter {
  override def failIfDifferentStrings(actual: String, expected: String, msg: String) {
    if( actual != expected ) throw new ComparisonFailure(msg, expected, actual)
  }
}

trait JunitMatchers extends Matchers with JunitFailureReporter with ToMatcherOps
object junitMatchers extends JunitMatchers
