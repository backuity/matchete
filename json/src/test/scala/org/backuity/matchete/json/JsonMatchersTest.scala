package org.backuity.matchete.json

import org.backuity.matchete.JunitMatchers
import org.junit.{ComparisonFailure, Test}

class JsonMatchersTest extends JunitMatchers with JsonMatchers {

  @Test
  def equalJsonShouldThrowReadableErrors(): Unit = {
    {

      """{
        |  "firstName": "john",
        |  "lastName":"doe", "age":43
        |}
      """.stripMargin must equalJson(
        """
          |{
          |  "nationality": "cowboy",
          |  "firstName": "john doe"
          |}
        """.stripMargin)

    } must throwA[ComparisonFailure].`with`("a readable message") {
      case err =>
        err.getExpected must_==
          """{
            |  "firstName":"john doe",
            |  "nationality":"cowboy"
            |}""".stripMargin

        err.getActual must_==
          """{
            |  "age":43,
            |  "firstName":"john",
            |  "lastName":"doe"
            |}""".stripMargin

        err.getMessage must_==
          """Non matching JSON.
            |Expected: {
            |            "firstName":"john doe",
            |            "nationality":"cowboy"
            |          }
            |Actual:   {
            |            "age":43,
            |            "firstName":"john",
            |            "lastName":"doe"
            |          }
            |Diff:
            |  Changed: {
            |             "firstName":"john"
            |           }
            |  Deleted: {
            |             "nationality":"cowboy"
            |           }
            |  Added:   {
            |             "age":43,
            |             "lastName":"doe"
            |           }
            |
            | expected:<{
            |  "[firstName":"john doe",
            |  "nationality":"cowboy]"
            |}> but was:<{
            |  "[age":43,
            |  "firstName":"john",
            |  "lastName":"doe]"
            |}>""".stripMargin

    }
  }

  @Test
  def nullOutJsonFields(): Unit = {
    nullOutJsonField("age", "id")(
      """{
        |  "name":"John",
        |  "age":123,
        |  "id":"1234-5678",
        |  "city":"New York"
        |}
      """.stripMargin) must_==
      """{
        |  "name":"John",
        |  "age":0,
        |  "id":"???",
        |  "city":"New York"
        |}
      """.stripMargin
  }

  @Test
  def doNotPrintEmptyDiffSections(): Unit = {
    {
      """{
        |  "name":"john",
        |  "city":"lausanne"
        |}""".
        stripMargin must equalJson(
        """{
          |  "name":"mary",
          |  "city":"toulouse"
          |}""".stripMargin)
    } must throwA[ComparisonFailure].`with`("empty Deleted and Added sections") {
      case err =>
        err.getMessage must_==
          """Non matching JSON.
            |Expected: {
            |            "city":"toulouse",
            |            "name":"mary"
            |          }
            |Actual:   {
            |            "city":"lausanne",
            |            "name":"john"
            |          }
            |Diff:
            |  Changed: {
            |             "city":"lausanne",
            |             "name":"john"
            |           }
            |
            | expected:<{
            |  "city":"[toulouse",
            |  "name":"mary]"
            |}> but was:<{
            |  "city":"[lausanne",
            |  "name":"john]"
            |}>""".stripMargin
    }
  }
}
