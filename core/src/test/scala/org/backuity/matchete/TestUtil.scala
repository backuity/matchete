package org.backuity.matchete

object TestUtil {

  case class Person(name: String, age: Int)
  case class Group(persons: List[Person]) {
    def isEmpty = persons.isEmpty
  }

  trait Stuff {
    def name: String
    def price: Int
  }
  case class Flower(name: String, price: Int) extends Stuff
  case class Bike(name: String, price: Int, brand: String) extends Stuff
  case class Bucket(stuffs: List[Stuff])

  final class CustomEqual(val str: String, val int: Int) {
    override def equals(other: Any): Boolean = {
      other match {
        case otherC: CustomEqual =>
          otherC.str == this.str &&
            otherC.int == this.int
      }
    }

    override def hashCode(): Int = 17 * (str.hashCode + 17 * int.hashCode)
    override def toString = s"CE($str,$int)"
  }

  class A
  case class A1() extends A
  case class A2() extends A

  class ASizedClass {
    def size = 12
  }
}