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
