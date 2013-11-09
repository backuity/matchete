package org.backuity.matchers

import scala.language.implicitConversions

/** Core matchers */
trait Matchers extends ExceptionMatchers
                  with StringMatchers
                  with TraversableMatchers
                  with OrderedMatchers
                  with NumericMatchers
                  with BooleanMatchers
                  with AnyMatchers {
}



