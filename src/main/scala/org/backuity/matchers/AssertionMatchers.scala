package org.backuity.matchers


trait AssertionFailureReporter extends FailureReporter {
  def fail(msg: String) = throw new AssertionError(msg)
}

/**
 * Matchers that throw [[java.lang.AssertionError]] upon failure.
 */
trait AssertionMatchers extends Matchers with AssertionFailureReporter with ToMatcherOps
object assertionMatchers extends AssertionMatchers
