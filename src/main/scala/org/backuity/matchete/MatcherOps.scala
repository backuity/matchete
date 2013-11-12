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


class MatcherOps[T](t : => T, reporter: FailureReporter) {

  private val anyMatchers = new AnyMatchers with FailureReporterDelegate {
    protected val failureReporterDelegate: FailureReporter = reporter
  }

  def must(matcher: Matcher[T])(implicit formatter: Formatter[T]) { matcher.check(t) }

  /** type safe equality */
  def must_==(other: T)(implicit formatter: Formatter[T]) { must(anyMatchers.beEqual(other))}

  /** type safe inequality */
  // note: we need a manifest for the not matcher
  def must_!=(other: T)(implicit formatter: Formatter[T], manifest: Manifest[T]) { must(anyMatchers.not(anyMatchers.beEqual(other)))}
}

trait ToMatcherOps { this : FailureReporter =>
  implicit def ToMatcherOpsFromAny[T](t : => T) = new MatcherOps[T](t, this)
}
