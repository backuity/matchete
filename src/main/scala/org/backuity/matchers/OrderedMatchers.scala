package org.backuity.matchers

trait OrderedMatchers extends CoreMatcherSupport {

  private def ordered[O](operator: (O,O) => Boolean, operatorDescription: String)(ref: O)(implicit formatter: Formatter[O]) = matcher[O](
    description = s"be $operatorDescription $ref",
    validate = operator(_,ref),
    failureDescription = (other: O) => s"${formatter.format(other)} is not $operatorDescription ${formatter.format(ref)}")

  def be_<[O : Ordering : Formatter](max : O) = ordered[O]( implicitly[Ordering[O]].lt, "<")(max)
  def be_>[O : Ordering : Formatter](min : O) = ordered[O]( implicitly[Ordering[O]].gt, ">")(min)
  def be_<=[O : Ordering : Formatter](max : O) = ordered[O]( implicitly[Ordering[O]].lteq, "<=")(max)
  def be_>=[O : Ordering : Formatter](min : O) = ordered[O]( implicitly[Ordering[O]].gteq, ">=")(min)
}
