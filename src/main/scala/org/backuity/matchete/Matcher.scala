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


trait Matcher[-T] { outer =>
  /** if t doesn't conform to the matcher then an exception will be raised
    * @return the evaluated `t` - this is used by the negation matcher
    */
  def check( t : => T) : Any

  def description : String

  override def toString = description

  def and[U <: T](other: Matcher[U]) : Matcher[U] = new Matcher[U] {
    def description = outer.description + " and " + other.description
    def check(t : => U ) : Any = {
      outer.check(t)
      other.check(t)
    }
  }

  def or[U <: T](other: Matcher[U])(implicit failureReporter: FailureReporter) : Matcher[U] = new Matcher[U] {
    def description = outer.description + " or " + other.description
    def check(t : => U) : Any = {
      (try {
        outer.check(t)
        None
      } catch {
        case e : Throwable => Some(e.getMessage)
      },
      try {
        other.check(t)
      } catch {
        case e : Throwable => Some(e.getMessage)
      }) match {
        case (Some(e1), Some(e2)) => failureReporter.fail(e1 + " and " + e2)
        case _ =>
      }
    }
  }
}

trait EagerMatcher[-T] extends Matcher[T] {
  protected def eagerCheck(t : T)

  final def check(t : => T) : Any = {
    val eval = t
    eagerCheck(eval)
    eval
  }
}
