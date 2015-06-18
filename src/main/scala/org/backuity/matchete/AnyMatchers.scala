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

import org.backuity.matchete.Diffable.{NestedDiff, BasicDiff, Equal, DiffResult}

import scala.language.reflectiveCalls

trait Sized[T] {
  def size(t: T) : Int
}

trait MatcherComparator[-T] {
  def checkEqual(actual: T, expected : T) : Unit
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
        def failWithHint(hint: String) = fail(s"${setFormatter.format(actualSet)} is not equal to ${setFormatter.format(expectedSet)}, $hint")

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

  implicit def anyComparator[T](implicit formatter: Formatter[T], diffable: Diffable[T]) = new MatcherComparator[T] {
    def checkEqual(actual: T, expected: T) : Unit = {
      val diff: DiffResult = diffable.diff(actual, expected)
      diff match {
        case Equal => // no-op
        case _ : BasicDiff => fail(s"${formatter.format(actual)} is not equal to ${formatter.format(expected)}")
        case nestedDiff : NestedDiff =>
          val msg = s"${formatter.format(actual)} is not equal to ${formatter.format(expected)}\n" +
                      s"${nestedDiff.pathValueA} ≠ ${nestedDiff.pathValueB}"
          nestedDiff.valueA match {
            case valueAString: String => failIfDifferentStrings(valueAString, nestedDiff.valueB.asInstanceOf[String], msg)
            case _ => fail(msg)
          }
      }
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

  def beEmpty[T](implicit ev1: T => {def isEmpty: Boolean}, formatter: Formatter[T]): Matcher[T] = be("empty") {
    case t if t.isEmpty =>
  }

  def beEq[T <: AnyRef](expected: T)(implicit formatter: Formatter[T]) = new EagerMatcher[T] {
    def description: String = "be eq " + expected

    private def format(obj: T) : String = s"${formatter.format(obj)} (${System.identityHashCode(obj)})"

    protected def eagerCheck(t : T) {
      failIf( t ne expected, s"${format(t)} is not eq ${format(expected)}" )
    }
  }

  def beNull[T <: AnyRef](implicit formatter: Formatter[T]) = new EagerMatcher[T] {
    protected def eagerCheck(t: T) {
      failIf(t != null, s"${formatter.format(t)} is not null")
    }
    def description: String = "be null"
  }

  // we need a manifest to be able to format the failure if the failing result is formatable
  def not[T](matcher: Matcher[T])(implicit formatter: Formatter[T], manifest: Manifest[T]) = new Matcher[T]{
    def description = "not " + matcher.description
    def check(t : => T) : Any = {
      def failWith(formattedEval: String) = fail(formattedEval + " should not " + matcher.description)

      val evalT = try {
        t
      } catch {
        case e : Throwable =>
          // let's see if our matcher can cope with that exception
          try {
            matcher.check { throw e }
          } catch {
            case matcherException if matcherException == e =>
              // matcher just forwarded the exception, let it pass through the not
              throw e

            case matcherException : Throwable =>
              // matcher failed with its own exception, that's correct
              return matcherException
          }

          // matcher can cope with `e`, but that's a `not` so fail it
          failWith(s"${e.getClass.getName}: ${e.getMessage}")
      }

      val eval = try {
        matcher.check(evalT)
      } catch {
        case e : Throwable => return e
      }

      if( eval == null ) {
        failWith("null")
      } else if( manifest.runtimeClass.isAssignableFrom(eval.getClass)) {
        failWith(formatter.format(eval.asInstanceOf[T]))
      } else {
        failWith(eval.toString)
      }
    }
  }

  def beA[T : Manifest] = matcher[Any](
    description = "be a " + manifest[T].runtimeClass.getCanonicalName,
    validate = (any: Any) => manifest[T].runtimeClass.isAssignableFrom(any.getClass),
    failureDescription = (any: Any) => s"$any is not a ${manifest[T].runtimeClass.getCanonicalName} it is a ${any.getClass.getCanonicalName}")

  def a[T : Manifest] = beA[T]
}
