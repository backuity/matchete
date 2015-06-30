package org.backuity.matchete.json

import org.backuity.matchete.JunitMatchers
import org.json4s.JsonAST.JNothing
import org.json4s.native.JsonParser
import org.junit.Test

class JsonStableRendererTest extends JunitMatchers with JsonStableRenderer {

  @Test
  def stableRendering(): Unit = {
    val json = JsonParser.parse(
      """{
        | "name":"john",
        | "city":"lausanne"
        |}""".stripMargin)
    pretty(json) must_==
      """{
        |  "city":"lausanne",
        |  "name":"john"
        |}""".stripMargin
  }

  @Test
  def renderNothing(): Unit = {
    pretty(JNothing) must_== ""
  }
}
