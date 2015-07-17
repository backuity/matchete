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


/** Helper to build the core matchers */
trait CoreMatcherSupport extends FailureReporter with ToMatcherOps {

  private class SimpleEagerMatcher[-T](val description: String, validate: T => Boolean, failureDescription: T => String) extends EagerMatcher[T] {
    protected def eagerCheck(t: T) {
      if( !validate(t) ) fail(failureDescription(t))
    }
  }

  private class NonNullSimpleEagerMatcher[-T](description: String, validate: T => Boolean, failureDescription: T => String, nullMessage: String) extends
    SimpleEagerMatcher[T](description, validate, failureDescription) {
    override protected def eagerCheck(t: T) {
      failIf(t == null, nullMessage)
      super.eagerCheck(t)
    }
  }

  private class PartialFunctionMatcher[-T](val description: String, descriptionNegation: String, pf: PartialFunction[T, Unit])(implicit formatter: Formatter[T]) extends EagerMatcher[T] {
    protected def eagerCheck(t: T) {
      if( pf.isDefinedAt(t) ) {
        try {
          pf(t)
        } catch {
          case util.control.NonFatal(e) =>
            val msg = new StringBuilder(formatter.format(t))
            msg.append(" ").append(descriptionNegation).append(":")
            if( !e.getMessage.startsWith("\n")) {
             msg.append(" ")
            }
            msg.append(e.getMessage)
            fail(msg.toString)
        }
      } else fail(s"${formatter.format(t)} $descriptionNegation")
    }
  }

  /**
   * A null accepting matcher.
   *
   * @param description should be the same as the matcher name, for instance haveSize(0) should be "have size 0"
   * @param validate function that returns true if the value matches the expectation
   * @param failureDescription called upon failure, return the message thrown
   */
  def nullAcceptingMatcher[T](description: String, validate: T => Boolean, failureDescription: T => String) : Matcher[T] =
    new SimpleEagerMatcher[T](description, validate, failureDescription)

  /**
   * A non-null matcher.
   *
   * @param description should be the same as the matcher name, for instance haveSize(0) should be "have size 0"
   * @param validate function that returns true if the value matches the expectation
   * @param failureDescription called upon failure, return the message thrown
   */
  def matcher[T](description: String, validate: T => Boolean, failureDescription: T => String, nullMessage : String = "") : Matcher[T] = {
    val msg = if( nullMessage == "" ) {
        if( description.startsWith("be") ) {
          "null is not" + description.substring(2)
        } else {
          "null does not " + description
        }
      } else nullMessage
    new NonNullSimpleEagerMatcher[T](description, validate, failureDescription, msg)
  }

  def partialFunctionMatcher[T](description: String, descriptionNegation: String)(pf: PartialFunction[T,Unit])(implicit formatter: Formatter[T]) : Matcher[T] =
    new PartialFunctionMatcher[T](description, descriptionNegation, pf)
  def partialFunctionMatcher[T](description: String)(pf: PartialFunction[T,Unit])(implicit formatter: Formatter[T]) : Matcher[T] =
    new PartialFunctionMatcher[T](description, "is not " + description, pf)

  def a[T : Formatter](description: String)(pf : PartialFunction[T,Unit]) : Matcher[T] = partialFunctionMatcher("a " + description)(pf)
  def an[T : Formatter](description: String)(pf : PartialFunction[T,Unit]) : Matcher[T] = partialFunctionMatcher("an " + description)(pf)

  def be[T : Formatter](description: String)(pf: PartialFunction[T,Unit]) : Matcher[T] = partialFunctionMatcher("be " + description, "is not " + description)(pf)
  def beLike[T : Formatter](description: String)(pf: PartialFunction[T,Unit]) : Matcher[T] = be("like " + description)(pf)

  def have[T : Formatter](description: String)(pf: PartialFunction[T,Unit]) : Matcher[T] = partialFunctionMatcher("have " + description, "does not have " + description)(pf)

  def be[T](m: Matcher[T]) = new EagerMatcher[T] {
    def description: String = "be " + m.description

    protected def eagerCheck(t: T) {
      m.check(t)
    }
  }
}

/**
 * Helper to build matchers. Provides core matchers.
 * @see [[org.backuity.matchete.Matchers]]
 */
trait MatcherSupport extends CoreMatcherSupport with Matchers