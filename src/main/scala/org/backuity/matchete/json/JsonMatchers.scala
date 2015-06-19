package org.backuity.matchete.json

import org.backuity.matchete.{EagerMatcher, Matcher, MatcherSupport}
import org.json4s.JsonAST.JString
import org.json4s._
import org.json4s.native.JsonParser

import scala.util.control.NonFatal

trait JsonMatchers extends MatcherSupport with JsonStableRenderer {

  def parseJson(json: String): JValue = JsonParser.parse(json)

  def equalJson(json: String, transformer :String => String = (v) => v): Matcher[String] =
    equalJson(JsonParser.parse(json), transformer)

  private[this] def isDifferent(diff: Diff): Boolean = {
    diff.added == JNothing &&
      diff.changed == JNothing &&
      diff.deleted == JNothing
  }

  /** Create a string transformer that replace fields by known values.
    * Ex: given the following JSON
    * {{{
    *     {
    *       "id" : "1234-123123",
    *       "name" : "John",
    *       "age" : "123"
    *     }
    * }}}
    * The transformer obtained by this call:
    * {{{
    *     replaceJsonField(value = "???", names = "id", "age")
    * }}}
    * Will turn the JSON into
    * {{{
    *     {
    *       "id" : "???",
    *       "name" : "John",
    *       "age" : "???"
    *     }
    * }}}
    * */
  def replaceJsonField(value : String, names: String*) : String => String = {
    val pattern = ("\"(" + names.mkString("|") + ")\":\"([^\"]*)\"").r
    (data : String) => pattern replaceAllIn (data, "\"$1\":\"" + value + "\"")
  }

  /**
   * Create a string transformer that replace the values of string and
   * int fields respectively by "???" and 0.
   * @see [[JsonMatchers#replaceJsonField]]
   */
  def nullOutJsonField(names : String*) : String => String = {
    val numericPattern = ("\"(" + names.mkString("|") + ")\":([^\"]*),").r
    replaceJsonField("???", names : _*).andThen { data =>
      numericPattern replaceAllIn(data, "\"$1\":0,")
    }
  }

  def equalJValue(json: String): Matcher[JValue] =
    equalJValue(JsonParser.parse(json))

  def equalJValue(json: JValue) : Matcher[JValue]= new EagerMatcher[JValue] {

    override def description: String = "equal json " + pretty(json)

    override protected def eagerCheck(actualJson: JValue): Unit = {
      val diff@Diff(changed, added, deleted) = json.diff(actualJson)
      if (!isDifferent(diff)) {
        failIfDifferentStrings(
          actual = pretty(actualJson),
          expected = pretty(json),
          msg = "Non matching JSON.\n" +
            indentLine("Expected: ", pretty(json)) +
            indentLine("Actual:   ", pretty(actualJson)) +
            indentLine("Diff:", "") +
            (if (changed != JNothing) indentLine("  Changed: ", pretty(changed)) else "") +
            (if (deleted != JNothing) indentLine("  Deleted: ", pretty(deleted)) else "") +
            (if (added != JNothing) indentLine("  Added:   ", pretty(added)) else "") +
            "\n")
      }
    }
  }

  def equalJson(json: JValue, transformer :String => String): Matcher[String] = new EagerMatcher[String] {
    override def description: String = "equal json " + pretty(json)

    override protected def eagerCheck(actualJsonStr: String): Unit = {
      val actualTransformedJson = transformer(actualJsonStr)
      val actualJson = try {
        parseJson(actualTransformedJson)
      } catch {
        case NonFatal(e) =>
          failIfDifferentStrings(
            actual = actualTransformedJson,
            expected = pretty(json),
            msg = "Cannot parse JSON")
          throw e // this exception should never be thrown
      }
      equalJValue(json).check(actualJson)
    }
  }

  def indentLine(prefix: String, str: String): String = {
    prefix + indent(str, prefix.length) + "\n"
  }

  def indent(str: String, numberOfSpace: Int): String = {
    str.replaceAll("(?m)^", " " * numberOfSpace).trim
  }

  def containJson(id: String, elemMatcher: Matcher[JValue]) = matcher[JValue](
    description = s"contain json $id ${elemMatcher.description}",
    validate = jvalue => {
      try {
        elemMatcher.check(jvalue \ id); true
      } catch {
        case _: Throwable => false
      }
    },
    failureDescription = jvalue => {
      try {
        elemMatcher.check(jvalue \ id); ""
      } catch {
        case t: Throwable =>
          s"$jvalue $id value ${t.getMessage}"
      }
    }
  )

  def containUnorderedJsonValues(id: String, values: String*) = partialFunctionMatcher[JValue]("contain json element " + values) {
    case json =>
      val equalMatchers = values.map(v => equalJValue(v))
      (json \ id).children must containExactly(equalMatchers:_*)
  }

  def containJsonElement(id: String) = matcher[JValue](
    description = "contain json element " + id,
    validate = _ \ id != JNothing,
    failureDescription = jvalue => s"$jvalue does not contain $id")

  def containJsonElement(id: String, elemMatcher: Matcher[String]) = matcher[JValue](
    description = s"contain json element $id ${elemMatcher.description}",
    validate = jvalue => {
      jvalue \ id match {
        case JNothing => false
        case JString(value) => try {
          elemMatcher.check(value); true
        } catch {
          case _: Throwable => false
        }
        case _ => false
      }
    },
    failureDescription = jvalue => {
      jvalue \ id match {
        case JNothing => s"$jvalue does not contain $id"
        case JString(value) =>
          try {
            elemMatcher.check(value)
            ""
          } catch {
            case t: Throwable =>
              s"$jvalue $id value ${t.getMessage}"
          }
        case other =>
          s"$jvalue $id is not a string it is a $other"
      }
    }
  )
}
