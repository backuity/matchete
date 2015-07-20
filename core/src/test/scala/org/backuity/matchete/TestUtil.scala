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

  class A
  case class A1() extends A
  case class A2() extends A

  class ASizedClass {
    def size = 12
  }
}