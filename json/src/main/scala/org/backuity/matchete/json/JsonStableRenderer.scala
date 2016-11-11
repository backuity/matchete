package org.backuity.matchete.json

import org.json4s.JsonAST.{JArray, JBool, JDecimal, JDouble, JInt, JObject, JString}
import org.json4s._

/** A Json renderer that sort alphabetically Json object fields.
  * This is particularly handy when testing.
  */
trait JsonStableRenderer {

  import JsonStableRenderer._
  import Document._

  def compact(d: Document): String = Printer.compact(d)

  def compact(value: JValue): String = compact(render(value))

  def pretty(d: Document): String = Printer.pretty(d)

  def pretty(value: JValue): String = pretty(render(value))

  /** Renders JSON.
    * @see Printer#compact
    * @see Printer#pretty
    */
  def render(value: JValue): Document = value match {
    case null => text("null")
    case JBool(true) => text("true")
    case JBool(false) => text("false")
    case JDouble(n) => text(n.toString)
    case JDecimal(n) => text(n.toString)
    case JInt(n) => text(n.toString)
    case JLong(n) => text(n.toString)
    case JNull => text("null")
    case JNothing => text("")
    case JString(null) => text("null")
    case JString(s) => text("\"" + ParserUtil.quote(s) + "\"")
    case JArray(arr) => text("[") :: series(trimArr(arr).map(render)) :: text("]")
    case JSet(set) => text("[") :: series(set.toList.map(render)) :: text("]")
    case JObject(obj) =>
      val nested = break ::
        fields(sortFields(trimObj(obj)).map({ case (n, v) => text("\"" + ParserUtil.quote(n) + "\":") :: render(v)}))
      text("{") :: nest(2, nested) :: break :: text("}")
  }

  private def trimArr(xs: List[JValue]) = xs.filter(_ != JNothing)

  private def trimObj(xs: List[JField]) = xs.filter(_._2 != JNothing)

  private def sortFields(fields: List[JField]) = fields.sortBy(_._1)

  private def series(docs: List[Document]) = punctuate(text(","), docs)

  private def fields(docs: List[Document]) = punctuate(text(",") :: break, docs)

  private def punctuate(p: Document, docs: List[Document]): Document =
    if (docs.isEmpty) empty
    else docs.reduceLeft((d1, d2) => d1 :: p :: d2)
}

object JsonStableRenderer {

  // copy-pasted from scala.text as it has been deprecated

  import java.io.Writer

  case object DocNil extends Document
  case object DocBreak extends Document
  case class DocText(txt: String) extends Document
  case class DocGroup(doc: Document) extends Document
  case class DocNest(indent: Int, doc: Document) extends Document
  case class DocCons(hd: Document, tl: Document) extends Document

  /**
   * A basic pretty-printing library, based on Lindig's strict version
   * of Wadler's adaptation of Hughes' pretty-printer.
   *
   * @author Michel Schinz
   * @version 1.0
   */
  sealed abstract class Document {
    def ::(hd: Document): Document = DocCons(hd, this)
    def ::(hd: String): Document = DocCons(DocText(hd), this)
    def :/:(hd: Document): Document = hd :: DocBreak :: this
    def :/:(hd: String): Document = hd :: DocBreak :: this

    /**
     * Format this document on `writer` and try to set line
     * breaks so that the result fits in `width` columns.
     */
    def format(width: Int, writer: Writer) {
      type FmtState = (Int, Boolean, Document)

      def fits(w: Int, state: List[FmtState]): Boolean = state match {
        case _ if w < 0 =>
          false
        case List() =>
          true
        case (_, _, DocNil) :: z =>
          fits(w, z)
        case (i, b, DocCons(h, t)) :: z =>
          fits(w, (i,b,h) :: (i,b,t) :: z)
        case (_, _, DocText(t)) :: z =>
          fits(w - t.length(), z)
        case (i, b, DocNest(ii, d)) :: z =>
          fits(w, (i + ii, b, d) :: z)
        case (_, false, DocBreak) :: z =>
          fits(w - 1, z)
        case (_, true, DocBreak) :: z =>
          true
        case (i, _, DocGroup(d)) :: z =>
          fits(w, (i, false, d) :: z)
      }

      def spaces(n: Int) {
        var rem = n
        while (rem >= 16) { writer write "                "; rem -= 16 }
        if (rem >= 8)     { writer write "        "; rem -= 8 }
        if (rem >= 4)     { writer write "    "; rem -= 4 }
        if (rem >= 2)     { writer write "  "; rem -= 2}
        if (rem == 1)     { writer write " " }
      }

      def fmt(k: Int, state: List[FmtState]): Unit = state match {
        case List() => ()
        case (_, _, DocNil) :: z =>
          fmt(k, z)
        case (i, b, DocCons(h, t)) :: z =>
          fmt(k, (i, b, h) :: (i, b, t) :: z)
        case (i, _, DocText(t)) :: z =>
          writer write t
          fmt(k + t.length(), z)
        case (i, b, DocNest(ii, d)) :: z =>
          fmt(k, (i + ii, b, d) :: z)
        case (i, true, DocBreak) :: z =>
          writer write "\n"
          spaces(i)
          fmt(i, z)
        case (i, false, DocBreak) :: z =>
          writer write " "
          fmt(k + 1, z)
        case (i, b, DocGroup(d)) :: z =>
          val fitsFlat = fits(width - k, (i, false, d) :: z)
          fmt(k, (i, !fitsFlat, d) :: z)
        case _ =>
          ()
      }

      fmt(0, (0, false, DocGroup(this)) :: Nil)
    }
  }

  object Document {
    /** The empty document */
    def empty = DocNil

    /** A break, which will either be turned into a space or a line break */
    def break = DocBreak

    /** A document consisting of some text literal */
    def text(s: String): Document = DocText(s)

    /**
     * A group, whose components will either be printed with all breaks
     * rendered as spaces, or with all breaks rendered as line breaks.
     */
    def group(d: Document): Document = DocGroup(d)

    /** A nested document, which will be indented as specified. */
    def nest(i: Int, d: Document): Document = DocNest(i, d)
  }

  object Printer extends Printer

  // copy-pasted from json4s as it relies on deprecated scala.text
  trait Printer {
    import java.io._

    /** Compact printing (no whitespace etc.)
      */
    def compact(d: Document): String = compact(d, new StringWriter).toString

    /** Compact printing (no whitespace etc.)
      */
    def compact[A <: Writer](d: Document, out: A): A = {
      def layout(docs: List[Document]): Unit = docs match {
        case Nil                   =>
        case DocText(s) :: rs      => out.write(s); layout(rs)
        case DocCons(d1, d2) :: rs => layout(d1 :: d2 :: rs)
        case DocBreak :: rs        => layout(rs)
        case DocNest(_, d) :: rs   => layout(d :: rs)
        case DocGroup(d) :: rs     => layout(d :: rs)
        case DocNil :: rs          => layout(rs)
      }

      layout(List(d))
      out.flush
      out
    }

    /** Pretty printing.
      */
    def pretty(d: Document): String = pretty(d, new StringWriter).toString

    /** Pretty printing.
      */
    def pretty[A <: Writer](d: Document, out: A): A = {
      d.format(0, out)
      out
    }
  }
}