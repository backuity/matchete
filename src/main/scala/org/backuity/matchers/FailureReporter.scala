package org.backuity.matchers


trait FailureReporter {

  // useful for combining matchers, see Matcher.or
  implicit val failureReporter : FailureReporter = this

  def fail(msg: String) : Nothing
  final def failIf(expr: Boolean, msg: => String) {
    if(expr) fail(msg)
  }
  def failIfDifferentStrings(actual: String, expected: String, msg: String) {
    if( actual != expected ) fail(msg)
  }
}

trait FailureReporterDelegate extends FailureReporter {
  // implicit make it useful for combining matchers, see Matcher.or
  protected val failureReporterDelegate: FailureReporter

  def fail(msg: String) : Nothing = failureReporterDelegate.fail(msg)
  override def failIfDifferentStrings(actual: String, expected: String, msg: String) {
    failureReporterDelegate.failIfDifferentStrings(actual, expected, msg)
  }
}


