package org.backuity.matchers

trait BooleanMatchers extends CoreMatcherSupport {

  def beTrue = matcher[Boolean](
    description = "be true",
    validate = _ == true,
    failureDescription = _ => "is not true")

  def beFalse = matcher[Boolean](
    description = "be false",
    validate = _ == false,
    failureDescription = _ => "is true")
}
