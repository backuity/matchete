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

import org.backuity.matchete.Diffable.SomeDiff


trait FailureReporter {

  // useful for combining matchers, see Matcher.or
  implicit val failureReporter: FailureReporter = this

  def fail(msg: String): Nothing

  final def failIf(expr: Boolean, msg: => String): Unit = {
    if (expr) fail(msg)
  }

  def failIfDifferentStrings(actual: String, expected: String, msg: String): Unit = {
    if (actual != expected) fail(msg)
  }

  def fail[T](diff: SomeDiff[T])(implicit formatter: Formatter[SomeDiff[T]]): Unit = {
    val msg = formatter.format(diff)
    diff.valueA match {
      case aString: String => failIfDifferentStrings(aString, diff.valueB.asInstanceOf[String], msg)
      case _ => fail(msg)
    }
  }
}

trait FailureReporterDelegate extends FailureReporter {
  // implicit make it useful for combining matchers, see Matcher.or
  protected val failureReporterDelegate: FailureReporter

  def fail(msg: String): Nothing = failureReporterDelegate.fail(msg)

  override def failIfDifferentStrings(actual: String, expected: String, msg: String): Unit = {
    failureReporterDelegate.failIfDifferentStrings(actual, expected, msg)
  }
}


