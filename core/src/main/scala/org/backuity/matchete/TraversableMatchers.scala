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

import scala.annotation.tailrec
import org.backuity.matchete.TraversableMatchers.ContainError
import scala.util.control.Breaks


trait TraversableMatchers extends CoreMatcherSupport {

  def forAll[T](m: Matcher[T]) = new EagerMatcher[TraversableOnce[T]] {
    def description = s"for all ${m.description}"

    def eagerCheck(once: TraversableOnce[T]) {
      val elems = asTraversable(once)
      for (elem <- elems) {
        try {
          m.check(elem)
        } catch {
          case util.control.NonFatal(e) => fail(s"$elems is not valid: ${e.getMessage}")
        }
      }
    }
  }

  /**
    * Valid if all the matchers are contained.
    * @note an element might satisfy multiple matchers, it is the caller responsibility to not pass overlapping matchers
    */
  def contain[T](matchers: Matcher[T]*) = new EagerMatcher[TraversableOnce[T]] {
    def description = s"contain ${matchers.map(_.description).mkString(", ")}"

    def eagerCheck(once: TraversableOnce[T]) {
      checkAnElementForEveryMatcher(matchers, asTraversable(once))
    }
  }

  def containAny[T](matchers: Matcher[T]*) = new EagerMatcher[TraversableOnce[T]] {
    def description = s"contain any of ${matchers.map(_.description).mkString(", ")}"

    protected def eagerCheck(once: TraversableOnce[T]) {
      val elems = asTraversable(once)
      import Breaks._
      breakable {
        val errors = for (matcher <- matchers) yield {
          checkAnElementForAMatcher(matcher, elems) match {
            case None => break()
            case Some(err) => err
          }
        }
        failFor(elems, "does not contain any of", errors)
      }
    }
  }

  /**
    * Valid if the sizes match and there is
    *   - for each element a satisfied matcher
    *   - for each matcher an element satisfying it
    *
    * Things like these are therefore accepted:
    * {{{
    *   3 elements: e1, e2, e3
    *   3 matchers: m1, m2, m3
    *
    *   e1 matches m1
    *   e2 matches m1
    *   e3 matches m2, m3
    * }}}
    *
    * It is the caller responsibility to not pass overlapping matchers.
    */
  def containExactly[T](matchers: Matcher[T]*)(implicit formatter: Formatter[Traversable[T]]) = new EagerMatcher[TraversableOnce[T]] {
    def description = s"contain exactly (${matchers.map(_.description).mkString(", ")})"

    def eagerCheck(once: TraversableOnce[T]) {
      val elems = asTraversable(once)
      val tooFewTooMany = if (elems.size != matchers.size) Some("too few") else if (elems.size > matchers.size) Some("too many") else None
      val sizeErrorMessage: Option[String] = if (elems.size == matchers.size) None
      else {
        Some(s"has size ${elems.size} but expected size ${matchers.size}")
      }
      val failPrefix = sizeErrorMessage.map(_ + " -- ").getOrElse("")

      checkAMatcherForEveryElement(elems, failPrefix)
      checkAnElementForEveryMatcher(matchers, elems, failPrefix)

      for (sizeError <- sizeErrorMessage) {
        failFor(elems, sizeError, Nil)
      }
    }


    def checkAMatcherForEveryElement(elems: Traversable[T], failPrefix: String)(implicit formatter: Formatter[Traversable[T]]) {

      // stop at the first successful matcher and return Nil
      // return the error messages otherwise
      @tailrec
      def checkMatchers(elem: T, errors: List[String], matchers: Seq[Matcher[T]]): List[String] = {
        matchers match {
          case matcher +: remainingMatchers =>
            try {
              matcher.check(elem)
              Nil
            } catch {
              case e: Throwable =>
                var msg = e.getMessage
                if (msg.startsWith(elem.toString)) {
                  msg = msg.substring(elem.toString.length)
                }
                msg = msg.trim
                checkMatchers(elem, errors :+ e.getMessage, remainingMatchers)
            }

          case empty => errors
        }
      }

      val failingElems = (for (elem <- elems) yield {
        checkMatchers(elem, Nil, matchers) match {
          case Nil => None
          case errors => Some(new ContainError(elem.toString, errors))
        }
      }).flatten

      val reason = (if (failPrefix.isEmpty) "has " else failPrefix) + "unexpected elements"
      failIfNeededFor(elems, reason, failingElems)
    }
  }

  /**
    * Stop at the first successful element
    * @return Some(ContainError) if no element match matcher, None otherwise
    */
  private def checkAnElementForAMatcher[T](matcher: Matcher[T], elems: Traversable[T]): Option[ContainError] = {
    if (elems.isEmpty) {
      Some(new ContainError(matcher.description, Nil))
    } else {

      // stop at the first successful element and return Nil
      // return the error messages otherwise
      @tailrec
      def checkElems(matcher: Matcher[T], errors: List[String], elems: Seq[T]): List[String] = {
        elems match {
          case elem +: remainingElems =>
            try {
              matcher.check(elem)
              Nil
            } catch {
              case e: Throwable =>
                val msg = e.getMessage
                checkElems(matcher, errors :+ msg, remainingElems)
            }

          case empty => errors
        }
      }

      checkElems(matcher, Nil, elems.toSeq) match {
        case Nil => None
        case errors => Some(new ContainError(matcher.description, errors))
      }
    }
  }

  private def checkAnElementForEveryMatcher[T](matchers: Seq[Matcher[T]], elems: Traversable[T], failPrefix: String = "")(implicit formatter: Formatter[Traversable[T]]) {
    val failingMatchers = (for (matcher <- matchers) yield checkAnElementForAMatcher(matcher, elems.toSeq)).flatten

    failIfNeededFor(elems, failPrefix + "does not contain", failingMatchers)
  }

  private def failIfNeededFor[T](elements: Traversable[T], reasons: String, failingElems: Traversable[ContainError])(implicit formatter: Formatter[Traversable[T]]): Unit = {
    if (failingElems.nonEmpty) {
      failFor(elements, reasons, failingElems)
    }
  }

  private def failFor[T](elements: Traversable[T], reasons: String, failingElems: Traversable[ContainError])(implicit formatter: Formatter[Traversable[T]]) {
    val what = formatter.format(elements)
    val msg = new StringBuilder(what)

    val longElementList = Formatter.width(what) > 80

    if (longElementList) {
      msg.append("\n\n").append(reasons)
    } else {
      msg.append(" ").append(reasons)
    }

    if (failingElems.nonEmpty) {
      if (longElementList) {
        msg.append(":\n")
      } else {
        if (failingElems.size > 1) msg.append(":") else msg.append(" ")
      }

      if (failingElems.size > 1) {
        msg.append("\n- ")
      }
      msg.append(failingElems.mkString("\n- "))
    }

    fail(msg.toString)
  }

  private def asTraversable[T](once: TraversableOnce[T]): Traversable[T] = {
    once match {
      case traversable: Traversable[T] => traversable
      case nonTraversable => nonTraversable.toList
    }
  }
}

object TraversableMatchers {

  /**
    * A failing item in a contain check with possibly multiple causes.
    *
    * For example:
    * {{{
    *   - Person(andrea,17) : // the item
    *     * is not an adult: 17 is not >= 18 // 1st cause
    *     * is not a 4 letter name: 'andrea' has size 6 but expected size 4 // 2nd cause
    *     * is not a 'jo' starting name: 'andrea' does not start with 'jo'' // 3rd cause
    * }}}
    * @param errors can be empty
    */
  private class ContainError(val item: String, val errors: List[String]) {
    override def toString = {
      errors match {
        case Nil => item

        // inline for one element
        case hd :: Nil =>
          if (hd.startsWith(item)) {
            hd
          } else {
            item + " : " + hd
          }

        // multiple lines for more than one element
        case _ =>
          val trimedErrors = errors.map { e =>
            if (e.startsWith(item)) {
              e.substring(item.length).trim
            } else {
              e
            }
          }
          item + " :\n  * " + trimedErrors.mkString("\n  * ")
      }
    }
  }
}