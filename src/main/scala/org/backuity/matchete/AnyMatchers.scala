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

import scala.language.reflectiveCalls

trait Sized[T] {
  def size(t: T) : Int
}

trait MatcherComparator[-T] {
  def checkEqual(actual: T, expected : T)
}

trait AnyMatchers extends CoreMatcherSupport {

  implicit def seqComparator[T](implicit elemFormatter: Formatter[T], seqFormatter: Formatter[Seq[T]]) = new MatcherComparator[Seq[T]] {
    def checkEqual(actualSeq: Seq[T], expectedSeq: Seq[T]) {
      val expectedIt = expectedSeq.iterator
      val actualIt = actualSeq.iterator
      var idx = 1

      def failAtIndex(msg: String) = fail(s"${seqFormatter.format(actualSeq)} is not equal to ${seqFormatter.format(expectedSeq)}, at index $idx $msg")

      while(expectedIt.hasNext) {
        val expectedNext = expectedIt.next()
        if( ! actualIt.hasNext ) {
          failAtIndex(s"expected ${elemFormatter.format(expectedNext)} but got no element")
        } else {
          val actualNext = actualIt.next()
          if( expectedNext != actualNext ) {
            failAtIndex(s"expected ${elemFormatter.format(expectedNext)} but got ${elemFormatter.format(actualNext)}")
          }
        }
        idx += 1
      }
      if( actualIt.hasNext ) {
        failAtIndex(s"expected no element but got ${elemFormatter.format(actualIt.next())}")
      }
    }
  }

  implicit def setComparator[T](implicit elemFormatter: Formatter[T], setFormatter: Formatter[Set[T]] ) = new MatcherComparator[Set[T]] {
    def checkEqual(actualSet: Set[T], expectedSet: Set[T]) {
      if( actualSet != expectedSet ) {
        def failWithHint(msg: String) = fail(s"${setFormatter.format(actualSet)} is not equal to ${setFormatter.format(expectedSet)}, $msg")

        val missing = expectedSet -- actualSet
        val extra = actualSet -- expectedSet
        val missingMsg = if( missing.size > 1 ) {
            List(s"elements ${missing.mkString(", ")} are missing")
          } else if( missing.size == 1 ) {
            List(s"element ${missing.head} is missing")
          } else {
            Nil
          }
        val extraMsg = if( extra.size > 1 ) {
            List(s"elements ${extra.mkString(", ")} were not expected")
          } else if(extra.size == 1) {
            List(s"element ${extra.head} was not expected")
          } else Nil
        failWithHint((missingMsg ::: extraMsg).mkString(" and "))
      }
    }
  }

  implicit def arrayComparator[T](implicit arrayFormatter: Formatter[Array[T]]) = new MatcherComparator[Array[T]] {
    def checkEqual(actualArr: Array[T], expectedArr: Array[T]) {
      if( expectedArr.deep != actualArr.deep ) {
        fail(s"${arrayFormatter.format(actualArr)} is not equal to ${arrayFormatter.format(expectedArr)}")
      }
    }
  }

  implicit def stringComparator(implicit formatter: Formatter[String]) = new MatcherComparator[String] {
    def checkEqual(actual: String, expected: String) {
      failIfDifferentStrings(actual, expected, s"${formatter.format(actual)} is not equal to ${formatter.format(expected)}")
    }
  }

  implicit def anyComparator[T](implicit formatter: Formatter[T]) = new MatcherComparator[T] {
    def checkEqual(actual: T, expected: T) {
      failIf(actual != expected, s"${formatter.format(actual)} is not equal to ${formatter.format(expected)}")
    }
  }

  /** non-type safe equality, prefer must_== when possible */
  private def equal[T](prefix: String, expected: T)(implicit formatter: Formatter[T], comparator: MatcherComparator[T]) = new EagerMatcher[T]{
    def description = s"${prefix}equal to ${formatter.format(expected)}"

    def eagerCheck(actual: T) {
      comparator.checkEqual(actual, expected)
    }
  }

  def equalTo[T : Formatter : MatcherComparator](expected: T) = equal("", expected)
  def beEqualTo[T : Formatter : MatcherComparator](expected: T) = equal("be ", expected)
  def be_==[T : Formatter : MatcherComparator](expected : T) = equal("be ", expected)

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

  def beEmpty[T <% Any { def isEmpty : Boolean }](implicit formatter: Formatter[T]) : Matcher[T] = be("empty") {
    case t if t.isEmpty =>
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
