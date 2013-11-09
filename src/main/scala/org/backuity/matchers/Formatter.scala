package org.backuity.matchers


trait Formatter[-T] {
  def format(t : T) : String

  def formatAll(elems: Traversable[T]) : String = elems.map(format).mkString(", ")
}

object Formatter {
  def apply[T](f : T => String) : Formatter[T] = new Formatter[T] {
    def format(t: T ) : String = f(t)
  }
}

trait Formatters {
  implicit def anyFormatter[T] : Formatter[T] = new Formatter[T]{
    def format(t: T): String = t.toString
  }
  implicit val stringFormatter : Formatter[String] = new Formatter[String]{
    def format(t: String): String = s"'$t'"
  }
  implicit def arrayFormatter[T] : Formatter[Array[T]] = new Formatter[Array[T]]{
    def format(t: Array[T]): String = t.deep.toString
  }
  implicit def traversableFormatter[T](implicit formatter: Formatter[T]) : Formatter[Traversable[T]] = new Formatter[Traversable[T]] {
    def format(t: Traversable[T]): String = t.stringPrefix + "(" + t.map( elem => formatter.format(elem)).mkString(", ") + ")"
  }
}