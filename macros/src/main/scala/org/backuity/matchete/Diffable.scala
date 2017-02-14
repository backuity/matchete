package org.backuity.matchete

import org.backuity.matchete.Diffable.{BasicDiff, DiffResult, Equal}
import shapeless._

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
  * A type-class for comparing types possibly nested.
  *
  * There's a materializer that produces a Diffable for:
  *  - case classes - by diffing the case class members
  *  - non case classes - by simply using equals
  *
  * If the default Diffable isn't what you want you can also diff by fields:
  * {{{
  *   class Person(val name: String, val age: String)
  *   val personDiffable : Diffable[Person] = Diffable.forFields(_.name, _.age)
  * }}}
  */
trait Diffable[T] {

  /** @return a DiffResult that must be coherent with equals, that is,
    *         for all a,b : diff(a,b) != Equals iff a != b */
  def diff(a: T, b: T): DiffResult
}

object Diffable extends LabelledTypeClassCompanion[Diffable] {

  sealed trait DiffResult {
    def nest[T](a: T, b: T, origin: String): DiffResult = this match {
      case Equal => Equal
      case diff: SomeDiff[_] => NestedDiff(a, b, origin, diff)
    }
  }
  case object Equal extends DiffResult
  sealed trait SomeDiff[+T] extends DiffResult {
    def sourceA: T
    def sourceB: T

    def valueA: Any
    def valueB: Any
    def formattedValueA: String
    def formattedValueB: String

    /** @return a readable string containing the path of the source of the diff along with the value of the first element */
    def pathValueA: String = formatPathWithValue(formattedValueA)

    /** @return a readable string containing the path of the source of the diff along with the value of the second element */
    def pathValueB: String = formatPathWithValue(formattedValueB)

    def path: String = path0("")

    private[Diffable] def path0(prefix: String): String

    def reasons: List[String]

    private def formatPathWithValue(value: String): String = {
      if (path.isEmpty) value else path + " = " + value
    }
  }
  case class BasicDiff[+T](sourceA: T, sourceB: T, reasons: List[String] = Nil)(implicit formatter: Formatter[T]) extends SomeDiff[T] {
    def valueA = sourceA
    def valueB = sourceB
    def formattedValueA = formatter.format(sourceA)
    def formattedValueB = formatter.format(sourceB)
    private[Diffable] def path0(prefix: String): String = prefix
  }
  case class NestedDiff[+T](sourceA: T, sourceB: T, origin: String, detail: SomeDiff[T]) extends SomeDiff[T] {
    def valueA = detail.valueA
    def valueB = detail.valueB
    def formattedValueA = detail.formattedValueA
    def formattedValueB = detail.formattedValueB
    private[Diffable] def path0(prefix: String): String = {
      val newPrefix = if (prefix.isEmpty) origin
      else {
        prefix + {
          if (origin.isEmpty) ""
          else {
            "." + origin
          }
        }
      }
      detail.path0(newPrefix)
    }
    def reasons = detail.reasons
  }

  implicit def intDiffable: Diffable[Int] = primitiveDiffable
  implicit def booleanDiffable: Diffable[Boolean] = primitiveDiffable
  implicit def doubleDiffable: Diffable[Double] = primitiveDiffable
  implicit def floatDiffable: Diffable[Float] = primitiveDiffable
  implicit def stringDiffable: Diffable[String] = primitiveDiffable

  private def primitiveDiffable[T: Formatter] = new Diffable[T] {
    override def diff(a: T, b: T): DiffResult = if (a != b) BasicDiff(a, b) else Equal
  }

  implicit def setDiffable[T : Diffable : Formatter]: Diffable[Set[T]] = new Diffable[Set[T]] {
    override def diff(a: Set[T], b: Set[T]) = {
      val formatter = Formatter.traversableContentFormatter[T]
      val missingElements = b -- a
      val extraElements = a -- b

      if( missingElements.isEmpty && extraElements.isEmpty ) {

        Equal

      } else if( missingElements.size == 1 && extraElements.size == 1 ) {

        // special case, we'll diff that one element
        val extra = extraElements.head
        val missing = missingElements.head
        Diffable[T].diff(extra, missing) match {
          case Equal => BasicDiff(a,b) // heck they are different!

          case someDiff : SomeDiff[_] =>
            val stringDiff = org.backuity.matchete.StringUtil.diff(extra.toString, missing.toString)
            val label = if (stringDiff.distinct == ".") {
              "<some-element>"
            } else stringDiff
            NestedDiff(a,b,label,someDiff)
        }

      } else {
        var reasons : _root_.scala.collection.immutable.List[_root_.java.lang.String] = _root_.scala.collection.immutable.Nil
        if( missingElements.nonEmpty ) {
          reasons = ("missing elements: " + Formatter.indent(formatter.format(missingElements), 18, firstLine = false)) :: reasons
        }

        if( extraElements.nonEmpty ) {
          reasons = ("extra elements: " + Formatter.indent(formatter.format(extraElements), 16, firstLine = false)) :: reasons
        }


        BasicDiff(a,b,reasons)
      }
    }
  }

  implicit def mapDiffable[K, T: Diffable: Formatter]: Diffable[Map[K,T]] = new Diffable[Map[K, T]] {
    override def diff(a: Map[K, T], b: Map[K, T]) = {
      if( a != b ) {
        val missingKeys = (a.keySet -- b.keySet) ++ (b.keySet -- a.keySet)
        missingKeys.headOption match {
          case Some(missingKey) =>
            NestedDiff(a,b,"get(" + missingKey + ")", BasicDiff(a.get(missingKey),b.get(missingKey)))

          case None =>
            a.find {
              case (k, v) => b(k) != v
            } match {
              case Some((k,v)) =>
                Diffable[T].diff(a(k),b(k)) match {
                  case Equal => BasicDiff(a,b) // shouldn't happen
                  case diff : SomeDiff[_] => NestedDiff(a,b,"get(" + k + ")",diff)
                }

              case None => BasicDiff(a,b)
            }
        }
      } else {
        Equal
      }
    }
  }

  // for some reason this method won't work as implicit
  def seqLikeDiffable[COL[_] <: Seq[_], T: Diffable: Formatter]: Diffable[COL[T]] = new Diffable[COL[T]] {
    override def diff(a: COL[T], b: COL[T]): DiffResult = {
      if (a.size != b.size) {
        NestedDiff(a,b,"size", BasicDiff(a.size, b.size))
      } else {
        // TODO a traverse would be nicer...
        for (i <- a.indices) {
          Diffable[T].diff(a(i).asInstanceOf[T], b(i).asInstanceOf[T]) match {
            case Equal => // OKAY
            case diff: SomeDiff[Any] => return NestedDiff(a,b,"(" + i + ")",diff)
          }
        }
        Equal
      }
    }
  }

  implicit def listDiffable[T: Diffable: Formatter]: Diffable[List[T]] = seqLikeDiffable
  implicit def seqDiffable[T: Diffable: Formatter]: Diffable[Seq[T]] = seqLikeDiffable


  object typeClass extends LabelledTypeClass[Diffable] {
    override def coproduct[L, R <: Coproduct](name: String, cl: => Diffable[L], cr: => Diffable[R]): Diffable[:+:[L, R]] = new Diffable[:+:[L, R]] {

      override def diff(a: :+:[L, R], b: :+:[L, R]): DiffResult = {
        (a, b) match {
          case (Inl(al), Inl(bl)) => cl.diff(al, bl).nest(al, bl, "")
          case (Inr(ar), Inr(br)) => cr.diff(ar, br)
          case (Inr(x), Inl(y)) => BasicDiff(Coproduct.unsafeGet(x), y)
          case (Inl(x), Inr(y)) => BasicDiff(x, Coproduct.unsafeGet(y))
        }
      }
    }

    override def emptyCoproduct: Diffable[CNil] = (_: CNil, _: CNil) => Equal

    override def product[H, T <: HList](name: String, ch: Diffable[H], ct: Diffable[T]): Diffable[::[H, T]] = new Diffable[H :: T] {
      override def diff(a: ::[H, T], b: ::[H, T]): DiffResult = {
        ch.diff(a.head, b.head) match {
          case diff: SomeDiff[_] => NestedDiff(a.head, b.head, name, diff)
          case Equal => ct.diff(a.tail, b.tail)
        }
      }
    }

    override def emptyProduct: Diffable[HNil] = (_: HNil, _: HNil) => Equal

    override def project[F, G](instance: => Diffable[G], to: (F) => G, from: (G) => F): Diffable[F] =
      (a: F, b: F) => {
        instance.diff(to(a), to(b)).nest(a, b, "")
      }
  }

  /**
    * Generate a [[Diffable]] based on a list of fields for a type `T`:
    * {{{
    *   class Person(val name: String, val age: String)
    *   val personDiffable: Diffable[Person] = Diffable.forFields(_.name, _.age)
    * }}}
    *
    * @note the resulting Diffable is consistent with the type `T` equals, that is, for
    *       all `t1` and `t2` of type `T`, and all diffable `d` produced with `Diffable.forFields`,
    *       iff `t1 == t2` then `d.diff(t1,t2) == Equal` and iff `t1 != t2` then `d.diff(t1,t2) != Equal`.
    */
  def forFields[T](fields: (T => Any)*): Diffable[T] = macro diffableForFields[T]

  def diffableForFields[T: c.WeakTypeTag](c: blackbox.Context)(fields: c.Tree*): c.Tree = {
    import c.universe._
    val tpe = implicitly[c.WeakTypeTag[T]].tpe

    def diffField(func: c.Tree, name: c.TermName): c.Tree = {
      val fieldTpe = func.tpe match {
        case TypeRef(_, _, List(tpe, ftpe)) => ftpe
        case other => c.abort(func.pos, "Unexpected function type, expected " + tpe + " => _ but got " + func.tpe)
      }
      val (fieldNameA, fieldNameB) = (name.toString + "A", name.toString + "B")
      q"""
           val fA = a.$name
           val fB = b.$name

           implicitly[Diffable[$fieldTpe]].diff(fA, fB) match {
             case Equal => // OK
             case diff : SomeDiff[_] =>
               return NestedDiff(a,b,${name.toString},diff)
           }
       """
    }

    val checkFields = fields.map {
      case func@q"($_) => $_.$name" => diffField(func, name)
      case func@q"($_) => $_.$name()" => diffField(func, name)

      case _ => c.abort(c.enclosingPosition, "Expected only function definition")
    }

    q"""
        new _root_.org.backuity.matchete.Diffable[$tpe] {
          import _root_.org.backuity.matchete.Diffable
          import Diffable.{DiffResult,Equal,NestedDiff,SomeDiff,BasicDiff}

          def diff(a: $tpe, b: $tpe): DiffResult = {
            if( a == b ) {
               Equal
            } else {
              ..$checkFields
              BasicDiff(a, b)
            }
          }
        }
     """
  }
}

