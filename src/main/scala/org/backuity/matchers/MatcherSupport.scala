package org.backuity.matchers


class SimpleEagerMatcher[-T](val description: String, validate: T => Boolean, failureDescription: T => String, fail : String => Unit) extends EagerMatcher[T] {
  protected def eagerCheck(t: T) {
    if( !validate(t) ) fail(failureDescription(t))
  }
}

/** Helper to build the core matchers */
trait CoreMatcherSupport extends FailureReporter with Formatters with ToMatcherOps {

  /**
   * @param description should be the same as the matcher name, for instance haveSize(0) should be "have size 0"
   * @param validate function that returns true if the value matches the expectation
   * @param failureDescription called upon failure, return the message thrown
   */
  def matcher[T](description: String, validate: T => Boolean, failureDescription: T => String) : Matcher[T] =
    new SimpleEagerMatcher[T](description, validate, failureDescription, fail)
}

/**
 * Helper to build matchers. Provides core matchers.
 * @see [[org.backuity.matchers.Matchers]]
 */
trait MatcherSupport extends CoreMatcherSupport with Matchers