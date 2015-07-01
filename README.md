matchete [![Build Status](https://travis-ci.org/backuity/matchete.png?branch=master)](https://travis-ci.org/backuity/matchete)
========

Matchete is a collection of (test) matchers. It is simple, type-safe, concise but yet offers comprehensive error messages. It has no dependencies and is easy to extend.

I'll assume you're already familiar with matchers. If not you can take a look at hamcrest: <https://github.com/hamcrest/JavaHamcrest>


## Simple

### Simple to use

Import Matchete in your project:

* SBT
    ```
    libraryDependencies += "org.backuity" %% "matchete" % "1.16" % "test"
    ```
    
* Maven
    ```xml
    <dependency>
        <groupId>org.backuity</groupId>
        <artifactId>matchete_2.11</artifactId>
        <version>1.16</version>
        <scope>test</scope>
    </dependency>
    ```

You can check the latest version on [Maven Central](http://search.maven.org/#search|gav|1|g%3A%22org.backuity%22%20AND%20a%3A%22matchete_2.11%22).

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


### For collections

Collection matchers are often tricky to get right. Matchete has been especially polished in this area.

See `TraversableMatchersTest`

```scala
List(2,4,6,8) must forAll(be_<(10) and be("an even number") { case n if n % 2 == 0 => })
List(2,1) must containElements(1,2)

// Contain Exactly
//
// make sure each element in the collection is matched by a matcher
// AND each matcher matches an element
//
// Note that a matcher might match multiple elements:
//
Set(1,2) must containExactly(
  an("even number") { case n if n % 2 == 0 => }, 
  be_<(3))

List(1,2,3) must containAny(be_<(5), be_>(100), be_>(200))
```

### For exceptions

```scala
def bug() { throw new IllegalArgumentException("this is an error message") }

bug must throwAn[IllegalArgumentException]

bug must throwAn[IllegalArgumentException].withMessage("this is an error message")

bug must throwAn[IllegalArgumentException].withMessageContaining("error", "message")

parse() must throwA[ParsingError].`with`("a correct offset") { case ParsingError(msg,offset) => offset must_== 3 }

parse() must throwA[ParsingError].suchAs { case ParsingError(msg,offset) => offset must_== 3 }
```

### For value objects

When comparing data structures you often end-up doing a field-by-field comparison:
```scala
data.field1 must_== exectedField1
data.field2 must_== exectedField2
// ...
data.fieldN must_== exectedFieldN
```

Backuity does this for you, for free!

```scala
case class Person(name: String, age: Int, address: Address)
case class Address(street: String)

Person("john",12, Address("street")) must_== Person("john",12,Address("different street"))    
```
Will throw the following `ComparisonFailure` (with `JunitMatchers`):
```
org.junit.ComparisonFailure: Person(john,12,Address(street)) is not equal to Person(john,12,Address(different street))
address.street = street ≠ address.street = different street 
Expected :different street
Actual   :street
```  

And for non-case-classes, you can define your own `Diffable`:
```scala
class Person(val name: String, val age: Int, val address: Address)
class Address(val street: String)

implicit lazy val diffablePerson : Diffable[Person] = Diffable.forFields(_.name, _.age, _.address)
implicit lazy val diffableAddress : Diffable[Address] = Diffable.forFields(_.street)

new Person("john",12, new Address("street")) must_== new Person("john",12,new Address("different street"))
```
Will throw the following `ComparisonFailure` (with `JunitMatchers`):
```
org.junit.ComparisonFailure: org.backuity.matchete.XX$Person@46d56d67 is not equal to org.backuity.matchete.XX$Person@d8355a8
address.street = street ≠ address.street = different street 
Expected :different street
Actual   :street
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

I often create a "test-base" class that groups matchers of common interest:

```scala
abstract class CityTestBase extends JunitMatchers with PersonMatchers with CityMatchers with BuildingMatchers // ... you get the idea
```

Note that you couldn't use your matchers alone as they would need a `FailureReporter`. See `JunitMatchers` to understand how to provide "standalone" matchers.


### Throw different errors

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


## Why Matchete?

In the scala world the matchers offer come in
 - [specs2](http://etorreborre.github.io/specs2/)
 - [scala test](http://www.scalatest.org/)

Those are full blown test frameworks, and their matchers are tightly integrated with the framework, making them slightly more complex (to my taste at least!), and harder to use standalone.


## Ok.. but where the hell does the name come from??

If you're wondering then I guess you've missed a piece of cinematography! 
I can just highly recommend that you check [this](http://en.wikipedia.org/wiki/Machete_%28film%29) out.


