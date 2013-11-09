package org.backuity.matchers

import org.junit.{ComparisonFailure, Test}

class ExceptionMatchersTest extends JunitMatchers {

  // first check 'must throw' assertions with try catch
  // then use them for testing matchers themselves

  private def failExpectedAssertionError() {
    // a runtime exception as we expect an assertion error
    throw new RuntimeException("Expected an AssertionError but got nothing")
  }

  private def expectAssertionError(msg: String)( f : => Unit ) {
    try {
      f
      failExpectedAssertionError()
    } catch {
      case e : AssertionError =>
        if( e.getMessage != msg ) {
          throw new ComparisonFailure("Wrong AssertionError message", msg, e.getMessage)
        }
    }
  }

  @Test
  def throwAnShouldFailForWrongExceptionType() {
    def bug() {
      throw new IllegalArgumentException("ooops")
    }

    expectAssertionError("Expected java.lang.IllegalStateException but got java.lang.IllegalArgumentException: ooops") {
      // Note; there bugs related to implicit search broken with Nothing https://issues.scala-lang.org/browse/SI-4982
      //       So statements returning nothing cannot be implicitely converted to ToMustOps
      //
      //       {throw new IllegalArgumentException("oops")} must throwAn[IllegalStateException] // won't compile

      bug() must throwAn[IllegalStateException]
    }
  }

  @Test
  def throwAnShouldFailWhenNoFailureDetected() {
    def noBug() {}

    expectAssertionError("Expected java.lang.IllegalStateException but got no exception") {
      noBug() must throwAn[IllegalStateException]
    }
  }

  @Test
  def throwAnShouldPassForRightExceptionType() {
    def bug() {
      throw new IllegalArgumentException("ooops")
    }

    bug() must throwAn[IllegalArgumentException]
  }

  @Test
  def throwAnExceptionWithMessageShouldPassForRightMessage() {
    def bug() {
      throw new IllegalArgumentException("ooops")
    }

    bug() must throwAn[IllegalArgumentException].withMessage("ooops")
  }

  @Test
  def throwAnExceptionWithMessageShouldFailForWrongMessage() {
    def bug() {
      throw new IllegalArgumentException("ooops")
    }

    try {
      bug() must throwAn[IllegalArgumentException].withMessage("bla")
      failExpectedAssertionError()
    } catch {
      case e : ComparisonFailure =>
        require(e.getActual == "ooops", e.getActual)
        require(e.getExpected == "bla", e.getExpected)
    }
  }

  @Test
  def throwAnExceptionLikeShouldFailForNoException() {
    def noBug() {}

    expectAssertionError("Expected java.lang.IllegalStateException but got no exception") {
      noBug() must throwAn[IllegalStateException].like("an illegal state exception") {
        case e : IllegalStateException =>
      }
    }
  }

  @Test
  def throwAnExceptionLikeShouldFailForWrongException() {
    def wrongException() {
      throw new IllegalArgumentException("ooops")
    }

    expectAssertionError("Expected java.lang.IllegalStateException but got java.lang.IllegalArgumentException: ooops") {
      wrongException() must throwAn[IllegalStateException].like("an illegal state exception") {
        case e : IllegalStateException =>
      }
    }
  }

  @Test
  def throwAnExceptionLikeShouldFailForWrongAssertion() {
    def bug() {
      throw new IllegalStateException("some stuff to check")
    }

    // ok
    bug() must throwAn[IllegalStateException].like("an illegal state exception") {
      case e : IllegalStateException => e.getMessage must contain("check")
    }

    // ko
    expectAssertionError("java.lang.IllegalStateException: some stuff to check is not " +
      "an java.lang.IllegalStateException like a WTF illegal state exception: " +
      "'some stuff to check' does not contain 'WTF'") {
      bug() must throwAn[IllegalStateException].like("a WTF illegal state exception") {
        case e : IllegalStateException => e.getMessage must contain("WTF")
      }
    }
  }
}
