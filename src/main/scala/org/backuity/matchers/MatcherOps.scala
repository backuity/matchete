package org.backuity.matchers


class MatcherOps[T](t : => T, reporter: FailureReporter) {

  private val anyMatchers = new AnyMatchers with FailureReporterDelegate {
    protected val failureReporterDelegate: FailureReporter = reporter
  }

  def must(matcher: Matcher[T])(implicit formatter: Formatter[T]) { matcher.check(t) }

  /** type safe equality */
  def must_==(other: T)(implicit formatter: Formatter[T]) { must(anyMatchers.beEqual(other))}

  /** type safe inequality */
  // note: we need a manifest for the not matcher
  def must_!=(other: T)(implicit formatter: Formatter[T], manifest: Manifest[T]) { must(anyMatchers.not(anyMatchers.beEqual(other)))}
}

trait ToMatcherOps { this : FailureReporter =>
  implicit def ToMatcherOpsFromAny[T](t : => T) = new MatcherOps[T](t, this)
}
