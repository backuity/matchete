package org.backuity.matchers

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
object junitMatchers extends JunitMatchers
