package org.backuity.matchers

import java.io.File

trait FileMatchers extends CoreMatcherSupport {

  def exist : Matcher[File] = matcher[File](
    description = "exist",
    validate = _.exists(),
    failureDescription = _.getCanonicalPath + " do not exist")
}
