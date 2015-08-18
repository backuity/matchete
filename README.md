matchete [![Build Status](https://travis-ci.org/backuity/matchete.png?branch=master)](https://travis-ci.org/backuity/matchete) [<img src="https://img.shields.io/maven-central/v/org.backuity/matchete-junit_2.11*.svg?label=latest%20release%20for%202.11"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3Aorg.backuity%20a%3Amatchete-junit_2.11*)
========

Matchete is a collection of (test) matchers. It is simple, type-safe, concise but yet offers comprehensive error messages. It has no dependencies and is easy to extend.

- [Simple](#simple)
  - [Simple to use](#simple-to-use)
  - [Simple to understand](#simple-to-understand)
- [Type safety](#type-safety)
- [Convenient matchers](#convenient-matchers)
  - [For object graphs - welcome Diffable](#for-object-graphs---welcome-diffable)
  - [For collections](#for-collections)
  - [For exceptions](#for-exceptions)
- [Nice error messages](#nice-error-messages)
- [Easy to extend](#easy-to-extend)
  - [Create your own matchers](#create-your-own-matchers)
  - [Throw different exceptions](#throw-different-exceptions)
  - [Wrap it up : organize your test code with traits](#wrap-it-up--organize-your-test-code-with-traits)
- [Why Matchete?](#why-matchete)
  
I'll assume you're already familiar with matchers. If not you can take a look at hamcrest: <https://github.com/hamcrest/JavaHamcrest>


## Simple

### Simple to use

It can be used along with JUnit by either extending `org.backuity.matchete.JunitMatchers` or importing `org.backuity.matchete.junitMatchers`.

```scala
class MyTest extends JunitMatchers {

  @Test
  def myTest() {
    "hello" must_== "hello"
  }
}
```

Depending on the assertion, the matchers will throw `java.lang.AssertionError` or `org.junit.ComparisonFailure`.

Matchete can also be used without a testing framework: extend `org.backuity.matchete.AssertionMatchers` instead and your matchers will throw `java.lang.AssertionError` only.


### Simple to understand

A matcher (`org.backuity.matchete.Matcher`) is merely

```scala
trait Matcher[-T] {
  /** if t doesn't conform to the matcher then an exception will be raised */
  def check( t : => T) : Any

  def description : String
}
```


## Type-safety

Matchete is type-safe by default:

```scala
// each one of the following line produces a COMPILATION ERROR

"hello" must_== 5 // type mismatch found Int(5) required String
5 must_== 10.0 // type mismatch found Double(10.0) required Int
List(1,2,3) must_== Set(1,2,3) // type mismatch found scala.collection.immutable.Set[Int] required List[Int]
List(1,2,3) must_== Seq(1,2,3) // type mismatch found Seq[Int] required List[Int]
```

but

```scala
val name = "john"
name must_== "john" // OK
Seq(1,2,3) must_== List(1,2,3) // OK : List[Int] conforms to Seq[Int]
```


## Convenient matchers

### For object graphs - welcome Diffable

When comparing data structures you often end-up doing a field-by-field comparison:
```scala
data.field1 must_== expectedField1
data.field2 must_== expectedField2
// ...
data.fieldN must_== expectedFieldN
```

Or worse when they form a graph of objects

```scala
data.field1.field1a.field1a1 == expectedField1.field1a.field1a1
data.field1.field1a.field1a2 == expectedField1.field1a.field1a2
// ...
```

Backuity does this for you, for free!

```scala
case class Person(name: String, age: Int, address: Address)
case class Address(street: String)

Person("john",12, Address("rue de la paix")) must_== Person("john",12,Address("rue du bourg"))    
```
Will throw the following `ComparisonFailure` (with `JunitMatchers`):
```
org.junit.ComparisonFailure: Person(john,12,Address(street)) is not equal to Person(john,12,Address(different street))
Got      : address.street = 'rue de la paix'
Expected : address.street = 'rue du bourg'
```  

This is achieved by using a `Diffable` type class which is automatically generated at compile-time by a macro. The macro
[currently supports](macros/src/main/scala/org/backuity/matchete/Diffable.scala#L139) case classes and some collections (`Seq`,`Set`,`Map`). 

For non-case-classes, you can define your own `Diffable`:
```scala
class Person(val name: String, val age: Int, val address: Address)
class Address(val street: String)

implicit lazy val diffablePerson : Diffable[Person] = Diffable.forFields(_.name, _.age, _.address)
implicit lazy val diffableAddress : Diffable[Address] = Diffable.forFields(_.street)

new Person("john",12, new Address("rue de la paix")) must_== new Person("john",12,new Address("rue du bourg"))
```
Will throw the following `ComparisonFailure` (with `JunitMatchers`):
```
org.junit.ComparisonFailure: org.backuity.matchete.XX$Person@46d56d67 is not equal to org.backuity.matchete.XX$Person@d8355a8
Got      : address.street = 'rue de la paix'
Expected : address.street = 'rue du bourg'
```  

### For collections

When matching exactly a collection use `must_==`

```scala
List(Person("john",12), Person("mary",24)) must_== List(Person("john",12), Person("mary",24))
Set(2,1) must_== Set(1,2)
Map(1 -> Person("john",12)) must_== Map(1 -> Person("john",12))
```

For more detailed examples see the following test cases on [List or Seq](core/src/test/scala/org/backuity/matchete/SeqDiffableTest.scala), [Set](core/src/test/scala/org/backuity/matchete/SetDiffableTest.scala) or [Map](core/src/test/scala/org/backuity/matchete/MapDiffableTest.scala)

If you don't want to match accurately you also use one of  
  - `contain(matchers: Matcher*)` fail if one of `matchers` does not match an element
  - `containExactly(matchers: Matcher*)` fail if 
    * an element isn't matched by a matcher
    * a matcher does not match an element
  - `containAny(matchers: Matcher*)` fail if none of `matchers` matches an element
  - `forAll(matcher: Matcher)` fail if an element isn't matched by `matcher`

Some examples:
```scala
List(1,2,3) must contain(be_<(2))

Set(1,2) must containExactly(
  an("even number") { case n if n % 2 == 0 => }, 
  be_<(3))

List(1,2,3) must containAny(be_<(5), be_>(100), be_>(200))

List(2,4,6,8) must forAll(be_<(10) and be("an even number") { case n if n % 2 == 0 => })
```

For more examples see [here](core/src/test/scala/org/backuity/matchete/TraversableMatchersTest.scala).

### For exceptions

```scala
def bug() { throw new IllegalArgumentException("this is an error message") }

bug must throwAn[IllegalArgumentException]

bug must throwAn[IllegalArgumentException].withMessage("this is an error message")

bug must throwAn[IllegalArgumentException].withMessageContaining("error", "message")

parse() must throwA[ParsingError].`with`("a correct offset") { case ParsingError(msg,offset) => offset must_== 3 }

parse() must throwA[ParsingError].suchAs { case ParsingError(msg,offset) => offset must_== 3 }
```


## Nice error messages

Given

```scala
case class Person(name: String, age: Int)
```
The following match

```scala
List(Person("john", 28), Person("sophie", 12), Person("andrea", 17)) must containExactly(
      an("adult"){ case Person(_,age) => age must be_>=(18)},
      a("4 letters name"){ case Person(name,_) => name must haveSize(4)},
      a("'jo' starting name"){ case Person(name,_) => name must startWith("jo")})
```

Will produce

```
List(Person(john,28), Person(sophie,12), Person(andrea,17)) has unexpected elements:
- Person(sophie,12) :
  * is not an adult: 12 is not >= 18
  * is not a 4 letters name: 'sophie' has size 6 but expected size 4
  * is not a 'jo' starting name: 'sophie' does not start with 'jo'
- Person(andrea,17) :
  * is not an adult: 17 is not >= 18
  * is not a 4 letters name: 'andrea' has size 6 but expected size 4
  * is not a 'jo' starting name: 'andrea' does not start with 'jo'
```

Formatting is done by a `Formatter` type class which can be customized for your own data structures.

```
implicit val customPersonFormatter = Formatter[Person]{ _.name }

Person("john", 12) must_== Person("mary", 24)
```        

Will produce

```
john is not equal to mary
Got     : age = 12
Expected: age = 24
```      


## Easy to extend

### Create your own matchers

Extend `MatcherSupport`, it provides few utilities to build matchers, depending on the level of customization you need.

For instance we could alias `an("adult")` into a `PersonMatchers` trait :

```scala
trait PersonMatchers extends MatcherSupport {

  def anAdult : Matcher[Person] = an("adult") { case Person(_,age) => age must be_>=(18) }
}
```

but if you need more control on the error message then you can use `matcher`:

```scala
  def anAdult = matcher[Person](
    description = "an adult",
    validate = _.age >= 18,
    failureDescription = person => s"$person is not an adult, its age (${person.age}) should be greater or equal to 18")
```

of course you can defined parameterized matchers:

```scala
  def anAdult(ageLimit: Int) : Matcher[Person] = an("adult") { case Person(_,age) => age must be >=(ageLimit) }
```

You can take a look at `FileMatchers` which isn't part of the core matchers.

Finally to use your matchers, just mix them in your tests:

```scala
class MyTests extends JunitMatchers with PersonMatchers { /* ... */ }
```


### Throw different exceptions

Say you want the matchers to throw `RuntimeException` instead of `AssertionError`. Easy, just implement `FailureReporter`:
```scala
trait FailureReporter {

  def fail(msg: String) : Nothing

  def failIfDifferentStrings(actual: String, expected: String, msg: String) {
    if( actual != expected ) fail(msg)
  }
}
```
`failIfDifferentStrings` can also be overriden if you need to throw different error messages for string mismatch.


### Wrap it up : organize your test code with traits

I often create a "test-base" trait that groups matchers, diffables and formatters of common interest:

```scala
trait CityTestBase extends JunitMatchers 
                      with PersonMatchers 
                      with CityMatchers 
                      with BuildingMatchers /* etc... */ {

   implicit val cityDiffable = Diffable.forFields(_.buildings,_.name)                      
   
   implicit val personFormatter = Formatter[Person]{ _.name } 

   // more diffable, formatters ...                      
}
```

## Why Matchete?

In the scala world the matchers offer come in
 - [specs2](http://etorreborre.github.io/specs2/)
 - [scala test](http://www.scalatest.org/)

Those are full blown test frameworks, and their matchers are tightly integrated with the framework, making them slightly more complex (to my taste at least!), and harder to use standalone.


### Ok.. but where the hell does the name come from??

If you're wondering then I guess you've missed a piece of cinematography! 
I can just highly recommend that you check [this](http://en.wikipedia.org/wiki/Machete_%28film%29) out.


