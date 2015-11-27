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

trait ThrowMatcher[T <: Throwable] extends Matcher[Any] {
  def withMessage(expectedMessage: String): Matcher[Any]
  def like(desc: String)(pf: PartialFunction[T, Unit]): Matcher[Any]
}

trait ExceptionMatchers {
  this: FailureReporter =>


  private def throwMatcher[T <: Throwable : Manifest](article: String) = new ThrowMatcher[T] {
    def expectedException = manifest[T].runtimeClass.getCanonicalName

    def description = s"throw $article $expectedException"

    private def checkThrowable(t: => Any)(furtherCheck: T => Unit): Throwable = {
      val exception = try {
        t
        None // obviously we cannot fail here otherwise it will be caught by the try-catch
      } catch {
        case err: Throwable =>
          if (!manifest[T].runtimeClass.isAssignableFrom(err.getClass)) {
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

    def withMessage(expectedMessage: String): Matcher[Any] = new Matcher[Any] {
      def description = s"throw $article $expectedException with message '$expectedMessage'"

      def check(t: => Any) = {
        checkThrowable(t) { err =>
          failIfDifferentStrings(err.getMessage, expectedMessage,
            s"Expected $expectedException message '$expectedMessage' but got '${err.getMessage}'")
        }
      }
    }

    def withMessageContaining(content: String*): Matcher[Any] = new Matcher[Any] {
      def description = s"throw $article $expectedException with message containing '$content'"

      def check(t: => Any) = {
        checkThrowable(t) { err =>
          val absents = content.filterNot(err.getMessage.contains).map("'" + _ + "'")
          failIf(!absents.isEmpty,
            s"Exception $expectedException with message '${err.getMessage}' does not contain ${absents.mkString(", ")}")
        }
      }
    }

    def like(desc: String, failMsg: String)(pf: PartialFunction[T, Unit]): Matcher[Any] = new Matcher[Any] {
      def description = s"throw $article $expectedException$desc"

      def check(t: => Any) = {
        checkThrowable(t) { err =>
          failIf(!pf.isDefinedAt(err), s"Exception $err$failMsg")
          try {
            pf(err)
          } catch {
            case e: Throwable =>
              fail(s"$err is not $article $expectedException$desc: ${e.getMessage}")
          }
        }
      }
    }

    def like(desc: String)(pf: PartialFunction[T, Unit]): Matcher[Any] = {
      like(" like " + desc, " is not like " + desc)(pf)
    }
    def `with`(desc: String)(pf: PartialFunction[T, Unit]): Matcher[Any] = {
      like(" with " + desc, " does not have " + desc)(pf)
    }
    def suchAs(pf: PartialFunction[T, Unit]): Matcher[Any] = {
      like("", " does not match")(pf)
    }
  }

  /** fail if the thrown exception isn't a `T` or a `T` subclass */
  def throwA[T <: Throwable : Manifest] = throwMatcher[T]("a")

  //  def throwA[T <: Throwable : Manifest : Equal](exception: T) =

  /** fail if the thrown exception isn't a `T` or a `T` subclass */
  def throwAn[T <: Throwable : Manifest] = throwMatcher[T]("an")
}