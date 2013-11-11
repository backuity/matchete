package org.backuity.matchers

import java.io.File

trait FileMatchers extends MatcherSupport {

  def exist : Matcher[File] = matcher[File](
    description = "exist",
    validate = _.exists(),
    failureDescription = _.getCanonicalPath + " do not exist")

  def haveLastModified(time: Long) : Matcher[File] = matcher[File](
    description = "have last-modified " + time,
    validate = _.lastModified() == time,
    failureDescription = file => s"File ${file.getCanonicalPath} has not been last-modified at $time but at ${file.lastModified()}"
  )
}
