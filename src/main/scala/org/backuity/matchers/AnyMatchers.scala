package org.backuity.matchers

import scala.language.reflectiveCalls

trait Sized[T] {
  def size(t: T) : Int
}

trait AnyMatchers extends CoreMatcherSupport {

  /** non-type safe equality, prefer must_== when possible */
  def beEqual[T](expected: T)(implicit formatter: Formatter[T]) = new EagerMatcher[T]{
    def description = s"be equal to ${formatter.format(expected)}"

    def eagerCheck(actual: T) {
      val notEqualMessage = s"${formatter.format(actual)} is not equal to ${formatter.format(expected)}"
      if( expected.isInstanceOf[String] ) {
        failIfDifferentStrings(actual.asInstanceOf[String], expected.asInstanceOf[String], notEqualMessage)
      } else {
        val same = (expected, actual) match {
          case (expectedArr: Array[_], actualArr: Array[_]) => expectedArr.deep == actualArr.deep
          case _ => expected == actual
        }
        failIf( !same, notEqualMessage)
      }
    }
  }

  def beEqualTo[T](expected: T) = beEqual(expected)
  def be_==[T](expected : T) = beEqual(expected)

  def be[T](m: Matcher[T]) = new EagerMatcher[T] {
    def description: String = "be " + m.description

    protected def eagerCheck(t: T) {
      m.check(t)
    }
  }

  implicit val SizedString : Sized[String] = new Sized[String] { def size(s: String) = s.length }
  implicit def SizedArray[T] : Sized[Array[T]] = new Sized[Array[T]] { def size(a: Array[T]): Int = a.length }
  implicit def SizedStructural[T <: { def size : Int }] : Sized[T] = new Sized[T] { def size(s : T) = s.size }

//  implicit val StringFormatter : Formatter[String] = new Formatter[String] { def format(s: String) = s"'$s'" }
//  implicit def AnyFormatter[T] : Formatter[T] = new Formatter[T] { def format(any: T) = any.toString }

  def haveSize[T](size: Int)(implicit sized: Sized[T], formatter: Formatter[T]) = new EagerMatcher[T] {
    def description = s"have size $size"
    def eagerCheck(t: T) {
      val actualSize = sized.size(t)
      failIf( actualSize != size, s"${formatter.format(t)} has size $actualSize but expected size $size")
    }
  }

  /**
   * beLike needs a description to have something to report in case of error.
   * Without a description nested beLike would make the test results hard to understand in some situation:
   * `containExactly(beLike(), beLike()) => "No element matched ???"`
   */
  def beLike[T](desc: String)( pf: PartialFunction[T,Unit]) = new EagerMatcher[T] {
    def description = desc
    def eagerCheck(t: T) {
      if( pf.isDefinedAt(t) ) {
        try {
          pf(t)
        } catch {
          case util.control.NonFatal(e) => fail(s"$t is not $description: ${e.getMessage}")
        }
      } else fail(s"$t is not $description")
    }
  }

  def beEmpty[T <% Any { def isEmpty : Boolean }](implicit formatter: Formatter[T]) = new EagerMatcher[T] {
    def description = "be empty"
    def eagerCheck(t : T) { failIf(!t.isEmpty, s"${formatter.format(t)} is not empty")}
  }

  // we need a manifest to be able to format the failure if the failing result is formatable
  def not[T : Manifest : Formatter](matcher: Matcher[T]) = new Matcher[T]{
    def description = "not " + matcher.description
    def check(t : => T) = {
      val res = try {
        Left(matcher.check(t))
      } catch {
        case e : Throwable => Right(e)
      }
      
      def failWith(formattedEval: String) { fail(formattedEval + " should not " + matcher.description)}
      
      res match {
        case Left(eval) =>
          if( manifest[T].runtimeClass.isAssignableFrom(eval.getClass)) {
            failWith(implicitly[Formatter[T]].format(eval.asInstanceOf[T]))
          } else {
            failWith(eval.toString)
          }

        case Right(e) => e
      }
    }
  }


  def beA[T : Manifest] = matcher[Any](
    description = "be a " + manifest[T].runtimeClass.getCanonicalName,
    validate = (any: Any) => manifest[T].runtimeClass.isAssignableFrom(any.getClass),
    failureDescription = (any: Any) => s"$any is not a ${manifest[T].runtimeClass.getCanonicalName} it is a ${any.getClass.getCanonicalName}")

  def a[T : Manifest] = beA[T]
}
