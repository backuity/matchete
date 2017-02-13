package org.backuity.matchete.scalatest

import org.backuity.matchete.Matchers
import org.scalatest.Suite

trait ScalaTestMatchers extends Matchers { this: Suite =>

  override def fail(msg: String): Nothing = {
    fail()
  }
}
