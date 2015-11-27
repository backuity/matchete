package org.backuity.matchete

import scala.collection.{mutable, Seq, GenSeq}

object StringUtil {

  def diff(a: String, b: String): String = {
    if (a.length > b.length) doDiff(a, b) else doDiff(b, a)
  }

  private def doDiff(a: String, b: String): String = {
    val occ = occCounts(b.seq)
    val str = new StringBuilder
    for (x <- a)
      if (occ(x) > 0) {
        str += x
        occ(x) -= 1
      } else {
        str += '.'
      }

    str.result()
  }

  private def occCounts(sq: Seq[Char]): mutable.Map[Char, Int] = {
    val occ = new mutable.HashMap[Char, Int] { override def default(k: Char) = 0 }
    for (y <- sq) occ(y) += 1
    occ
  }
}
