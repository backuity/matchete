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

case class Delta[S : Numeric](n: S, delta: S) {
  import Numeric.Implicits._
  import Ordered._

  val inf = n - delta
  val sup = n + delta

  def includes(s: S) : Boolean = {
    s >= inf && s <= sup
  }

  def reason(s: S) : String = {
    if( s < inf ) s"$s is < $inf"
    else if( s > sup ) s"$s is > $sup"
    else s"$s is within [$inf; $sup]"
  }

  override def toString = s"$n +/- $delta"
}

class DeltaBuilder[T : Numeric](n : T) {
  def +/-(delta: T) : Delta[T] = Delta(n, delta)
}

trait NumericMatchers extends CoreMatcherSupport {

  implicit def toDeltaBuilder[T : Numeric](t : T) : DeltaBuilder[T] = new DeltaBuilder(t)

  def beCloseTo[T](delta: Delta[T]) : Matcher[T] = matcher[T](
    description = "be close to " + delta,
    validate = (t:T) => delta.includes(t),
    failureDescription = (t:T) => s"$t is not close to $delta (${delta.reason(t)})"
  )
}
