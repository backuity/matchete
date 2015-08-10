package org.backuity.matchete

import org.backuity.matchete.DiffableForFieldsTest.EqualOnField1
import org.junit.Test

class DiffableForFieldsTest extends JunitMatchers {

  @Test
  def diffableForFieldsShouldBeConsistentWithClassEquality(): Unit = {
    val obj1 = new EqualOnField1("ha", "ho")
    val obj2 = new EqualOnField1("ha", "he")
    val obj3 = new EqualOnField1("he", "he")

    {
      obj1 must_== obj2
      obj2 must_!= obj3
    }

    {
      implicit val diffable: Diffable[EqualOnField1] = Diffable.forFields[EqualOnField1](_.field2)

      obj1 must_== obj2
      obj2 must_!= obj3
    }
  }
}

object DiffableForFieldsTest {
  class EqualOnField1(val field1: String, val field2: String) {
    override def hashCode(): Int = 17 * field1.hashCode

    override def equals(other: Any): Boolean = {
      other match {
        case otherE : EqualOnField1 =>
          this.field1 == otherE.field1

        case _ => false
      }
    }
  }
}