package org.backuity.matchers


trait StringMatchers extends CoreMatcherSupport {

  def startWith(prefix: String)(implicit formatter: Formatter[String]) = matcher[String](
    description = s"start with $prefix",
    validate = _.startsWith(prefix),
    failureDescription = (t: String) => s"${formatter.format(t)} does not start with ${formatter.format(prefix)}")

  def contain(substring: String)(implicit formatter: Formatter[String]) = matcher[String](
    description = "contain " + substring,
    validate = _.matches(".*?" + substring + ".*?"),
    failureDescription = (t: String) => s"${formatter.format(t)} does not contain ${formatter.format(substring)}")
}