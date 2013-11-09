package org.backuity.matchers

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
