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
