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

trait ThrowMatcher[T <: Throwable] extends Matcher[Any] {
  def withMessage(expectedMessage: String) : Matcher[Any]
  def like(desc: String)(pf : PartialFunction[T,Unit]) : Matcher[Any]
}

trait ExceptionMatchers { this : FailureReporter =>


  private def throwMatcher[T <: Throwable : Manifest](article: String) = new ThrowMatcher[T] {
    def expectedException = manifest[T].runtimeClass.getCanonicalName

    def description = s"throw $article $expectedException"

    private def checkThrowable(t : => Any)(furtherCheck : T => Unit) : Throwable = {
      val exception = try {
        t
        None // obviously we cannot fail here otherwise it will be caught by the try-catch
      } catch {
        case err : Throwable =>
          if( ! manifest[T].runtimeClass.isAssignableFrom(err.getClass)) {
            fail(s"Expected $expectedException but got $err")
          }
          furtherCheck(err.asInstanceOf[T])
          Some(err)
      }

      exception match {
        case None => fail(s"Expected $expectedException but got no exception")
        case Some(e) => e
      }
    }

    def check(t: => Any) = {
      checkThrowable(t)(_ => ())
    }

    def withMessage(expectedMessage: String) : Matcher[Any] = new Matcher[Any] {
      def description = s"throw $article $expectedException with message '$expectedMessage'"

      def check(t : => Any) = {
        checkThrowable(t) { err =>
          failIfDifferentStrings( err.getMessage, expectedMessage,
            s"Expected ${expectedException} message '$expectedMessage' but got '${err.getMessage}'")
        }
      }
    }

    def like(desc: String)(pf : PartialFunction[T,Unit]) : Matcher[Any] = new Matcher[Any] {
      def description = s"throw $article $expectedException like $desc"

      def check(t : => Any) = {
        checkThrowable(t) { err =>
          failIf( !pf.isDefinedAt(err), s"Exception $err is not like $desc")
          try {
            pf(err)
          } catch {
            case e : Throwable =>
              fail(s"$err is not $article $expectedException like $desc: ${e.getMessage}")
          }
        }
      }
    }
  }

  def throwA[T <: Throwable: Manifest] = throwMatcher[T]("a")
  def throwAn[T <: Throwable : Manifest] = throwMatcher[T]("an")
}