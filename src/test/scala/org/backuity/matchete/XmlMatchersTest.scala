package org.backuity.matchete

import org.junit.{ComparisonFailure, Test}

class XmlMatchersTest extends JunitMatchers with XmlMatchers {

  val person = <person name="john" age="37">
        <person name="john jr. 1" age="12"/>
        <person name="john jr. 2" age="10"/>
        <comment>john is cool</comment>
      </person>

  @Test
  def haveAttributeTest() {
    person must haveAttribute("name")
    person must haveAttribute("age")

    {person must haveAttribute("sex")} must throwAn[AssertionError].withMessage(
      """<person age="37" name="john"> does not have the attribute 'sex'""")
  }

  @Test
  def haveMatchingAttributeTest() {
    person must haveAttribute("name", equalTo("john"))

    {person must haveAttribute("sex", equalTo("M"))} must throwAn[AssertionError].withMessage(
      """<person age="37" name="john"> does not have the attribute 'sex'""")

    {person must haveAttribute("name", equalTo("mary"))} must throwAn[AssertionError].withMessage(
      """<person age="37" name="john"> attribute 'name' is not valid: 'john' is not equal to 'mary' expected:<[mary]> but was:<[john]>""")
  }

  @Test
  def haveTextTest() {
    person \ "person" must haveTrimmedText("")
    person \ "comment" must haveTrimmedText("john is cool")

    {person \ "comment" must haveTrimmedText("nope")} must throwAn[AssertionError].withMessage(
      """<comment>john is cool</comment> does not have trimmed text 'nope': 'john is cool' is not equal to 'nope' expected:<[nope]> but was:<[john is cool]>""")
  }

  @Test
  def haveLabelTest() {
    person must haveLabel("person")
    (person \ "comment").head must haveLabel("comment")

    {person must haveLabel("buddy")} must throwAn[AssertionError].withMessage(
      person + " does not have label 'buddy': 'person' is not equal to 'buddy' expected:<[buddy]> but was:<[person]>"
    )
  }

  @Test
  def testingChildren() {
    person \ "person" must containExactly(
      a("john jr 1") { case jr => jr must haveAttribute("age", equalTo("10"))},
      a("john jr 2") { case jr => jr must haveAttribute("age", equalTo("12"))})

    {person \ "person" must containExactly(
      a("john jr 1") { case jr => jr must haveAttribute("age", equalTo("10"))},
      an("old junior") { case jr => jr must haveAttribute("age", equalTo("21"))})} must throwAn[AssertionError].withMessage(
      """<person name="john jr. 1" age="12"/><person name="john jr. 2" age="10"/> has unexpected elements <person name="john jr. 1" age="12"/> :
        |  * is not a john jr 1: <person age="12" name="john jr. 1"> attribute 'age' is not valid: '12' is not equal to '10' expected:<1[0]> but was:<1[2]>
        |  * is not an old junior: <person age="12" name="john jr. 1"> attribute 'age' is not valid: '12' is not equal to '21' expected:<[21]> but was:<[12]>
      """.stripMargin.trim)
  }
}
