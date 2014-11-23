# 1.11 _(2014-11-23)_

* Add `with` to exception matchers :
```scala
    parse() must throwA[ParsingError].`with`("a correct offset") { case ParsingError(msg,offset) => offset must_== 3 }
```
