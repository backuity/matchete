/*
 * Copyright 2013 Bruno Bieth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.backuity.matchete

import scala.xml.{NodeSeq, Node}

trait XmlMatchers extends MatcherSupport {

  implicit val nodeFormatter = new Formatter[Node] {
    def format(t: Node): String = t.toString()
  }
  implicit val nodeSeqFormatter = new Formatter[NodeSeq] {
    def format(t: NodeSeq): String = t.toString()
  }

  def haveLabel(label: String) : Matcher[Node] = have(s"label '$label'") {
    case node => node.label must_== label
  }

  def haveText(text: String) : Matcher[NodeSeq] = have(s"text '$text'") {
    case node => node.text must_== text
  }

  def haveTrimmedText(text: String) : Matcher[NodeSeq] = have(s"trimmed text '$text'") {
    case node => node.text.trim must_== text
  }

  private def nodeWithoutChildrenToString(node: Node) : String = {
    val attributes = node.attributes.asAttrMap
    val attributeKeys = attributes.keySet.toList.sorted
    val attributesToString = attributeKeys.map( k => k + "=\"" + attributes(k) + "\"")
    s"<${node.label} ${attributesToString.mkString(" ")}>"
  }

  /** Check whether an attribute is present, if not print the node 'header',
    * it's label along with its attributes and their values, sorted by attribute name */
  def haveAttribute(attrName: String) = matcher[Node](
    description = s"have attribute '$attrName'",
    validate = _.attribute(attrName).isDefined,
    failureDescription = node => s"${nodeWithoutChildrenToString(node)} does not have the attribute '$attrName'")

  def haveAttribute(attrName: String, matcher: Matcher[String]) = new EagerMatcher[Node] {
    def description = s"have attribute '$attrName' ${matcher.description}"

    def eagerCheck(node: Node) {
      node.attribute(attrName) match {
        case Some(value) =>
          try {
            matcher.check(value.text)
          } catch {
            case t : Throwable =>
              fail(s"${nodeWithoutChildrenToString(node)} attribute '$attrName' is not valid: ${t.getMessage}")
          }

        case None => fail(s"${nodeWithoutChildrenToString(node)} does not have the attribute '$attrName'")
      }
    }
  }
}
