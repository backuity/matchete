package org.backuity.matchers

trait AgeMatchers extends MatcherSupport {

  import CustomMatchersTest.Person

  def beAnAdult : Matcher[Int] = beLike("an adult") { case age => age must be_>=(18) }

  // type inference variation
  def beAnAdult2 = beLike[Int]("an adult") { case age => age must be_>=(18) }

  def beAnAdult3 = matcher[Person](
    description = "be an adult",
    validate = _.age >= 18,
    failureDescription = person => s"$person is not an adult, its age (${person.age}}) isn't >= 18"
  )
}

object CustomMatchersTest {
  case class Person(name: String, age: Int)
}

class CustomMatchersTest {

}
