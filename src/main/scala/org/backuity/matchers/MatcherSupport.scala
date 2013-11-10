package org.backuity.matchers


/** Helper to build the core matchers */
trait CoreMatcherSupport extends FailureReporter with Formatters with ToMatcherOps {

  private class SimpleEagerMatcher[-T](val description: String, validate: T => Boolean, failureDescription: T => String) extends EagerMatcher[T] {
    protected def eagerCheck(t: T) {
      if( !validate(t) ) fail(failureDescription(t))
    }
  }

  private class PartialFunctionMatcher[-T](val description: String, descriptionNegation: String, pf: PartialFunction[T, Unit])(implicit formatter: Formatter[T]) extends EagerMatcher[T] {
    protected def eagerCheck(t: T) {
      if( pf.isDefinedAt(t) ) {
        try {
          pf(t)
        } catch {
          case util.control.NonFatal(e) => fail(s"${formatter.format(t)} $descriptionNegation: ${e.getMessage}")
        }
      } else fail(s"${formatter.format(t)} $descriptionNegation")
    }
  }

  /**
   * @param description should be the same as the matcher name, for instance haveSize(0) should be "have size 0"
   * @param validate function that returns true if the value matches the expectation
   * @param failureDescription called upon failure, return the message thrown
   */
  def matcher[T](description: String, validate: T => Boolean, failureDescription: T => String) : Matcher[T] =
    new SimpleEagerMatcher[T](description, validate, failureDescription)

  def partialFunctionMatcher[T](description: String, descriptionNegation: String)(pf: PartialFunction[T,Unit])(implicit formatter: Formatter[T]) : Matcher[T] =
    new PartialFunctionMatcher[T](description, descriptionNegation, pf)
  def partialFunctionMatcher[T](description: String)(pf: PartialFunction[T,Unit])(implicit formatter: Formatter[T]) : Matcher[T] =
    new PartialFunctionMatcher[T](description, "is not " + description, pf)

  def a[T : Formatter](description: String)(pf : PartialFunction[T,Unit]) : Matcher[T] = partialFunctionMatcher("a " + description)(pf)
  def an[T : Formatter](description: String)(pf : PartialFunction[T,Unit]) : Matcher[T] = partialFunctionMatcher("an " + description)(pf)

  def be[T : Formatter](description: String)(pf: PartialFunction[T,Unit]) : Matcher[T] = partialFunctionMatcher("be " + description, "is not " + description)(pf)
  def beLike[T : Formatter](description: String)(pf: PartialFunction[T,Unit]) : Matcher[T] = be("like " + description)(pf)

  def have[T : Formatter](description: String)(pf: PartialFunction[T,Unit]) : Matcher[T] = partialFunctionMatcher("have " + description, "does not have " + description)(pf)

  def be[T](m: Matcher[T]) = new EagerMatcher[T] {
    def description: String = "be " + m.description

    protected def eagerCheck(t: T) {
      m.check(t)
    }
  }
}

/**
 * Helper to build matchers. Provides core matchers.
 * @see [[org.backuity.matchers.Matchers]]
 */
trait MatcherSupport extends CoreMatcherSupport with Matchers