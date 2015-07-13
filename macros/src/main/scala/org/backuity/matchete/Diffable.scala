package org.backuity.matchete

import org.backuity.matchete.Diffable.DiffResult

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
  def diff(a : T, b : T) : DiffResult
}

object Diffable {

  sealed trait DiffResult
  case object Equal extends DiffResult
  sealed trait SomeDiff extends DiffResult {

    /** @return a readable string containing the path of the source of the diff along with the value of the first element */
    val pathValueA : String = formatPathWithValue(_.sourceA)

    val valueA : Any = value(_.sourceA)

    /** @return a readable string containing the path of the source of the diff along with the value of the second element */
    val pathValueB : String = formatPathWithValue(_.sourceB)

    val valueB : Any = value(_.sourceB)

    private def formatPathWithValue(f : BasicDiff => Any, nestedPrefix : String = "", basicPrefix : String = "") : String = {
      this match {
        case basic: BasicDiff => basicPrefix + f(basic).toString
        case NestedDiff(_,_,path, nested) => nestedPrefix + path + nested.formatPathWithValue(f, ".", " = ")
      }
    }

    private def value(f : BasicDiff => Any) : Any = {
      this match {
        case basic: BasicDiff => f(basic)
        case NestedDiff(_,_,_,nested) => nested.value(f)
      }
    }
  }
  case class BasicDiff(sourceA: Any, sourceB: Any, reasons : List[String] = Nil) extends SomeDiff
  case class NestedDiff(sourceA: Any, sourceB: Any, origin: String, detail: SomeDiff) extends SomeDiff

  implicit def materializeDiffable[T] : Diffable[T] = macro materializeDiffImpl[T]

  def forFields[T]( fields : (T => Any)*) : Diffable[T] = macro diffableForFields[T]

  def diffableForFields[T : c.WeakTypeTag](c : blackbox.Context)(fields: c.Tree*) : c.Tree = {
    import c.universe._
    val tpe = implicitly[c.WeakTypeTag[T]].tpe

    def diffField(func: c.Tree, name: Any): c.Tree = {
      val fieldTpe = func.tpe match {
        case TypeRef(_,_, List(tpe, ftpe)) => ftpe
        case other => c.abort(func.pos, "Unexpected function type, expected " + tpe + " => _ but got " + func.tpe)
      }
      val (fieldNameA,fieldNameB) = (name.toString + "A", name.toString + "B")
      q"""
           val fA = $func(a)
           val fB = $func(b)

           implicitly[Diffable[$fieldTpe]].diff(fA, fB) match {
             case Equal => // OK
             case diff : SomeDiff =>
               return NestedDiff(a,b,${name.toString},diff)
           }
       """
    }

    val checkFields = fields.map {
      case func @ q"($_) => $_.$name" => diffField(func, name)
      case func @ q"($_) => $_.$name()" => diffField(func, name)

      case _ => c.abort(c.enclosingPosition, "Expected only function definition")
    }

    q"""
        new _root_.org.backuity.matchete.Diffable[$tpe] {
          import _root_.org.backuity.matchete.Diffable
          import Diffable.{DiffResult,Equal,NestedDiff,SomeDiff}

          def diff(a : $tpe, b : $tpe) : DiffResult = {
            ..$checkFields
            Equal
          }
        }
     """
  }

  def materializeDiffImpl[T: c.WeakTypeTag](c : blackbox.Context) : c.Tree = {
    import c.universe._
    val tag: WeakTypeTag[T] = implicitly[WeakTypeTag[T]]

    val diffLogic = if( tag.tpe.typeSymbol.isClass &&
                        tag.tpe.typeSymbol.asClass.isCaseClass ) {

      materializeCaseClassDiffable(c)(tag)
    } else if( tag.tpe <:< typeOf[Seq[_]] ) {
      materializeSeqDiffable(c)(tag)
    } else if( tag.tpe <:< typeOf[Map[_,_]]) {
      materializeMapDiffable(c)(tag)
    } else if( tag.tpe <:< typeOf[Set[_]]) {
      materializeSetDiffable(c)(tag)
    } else {
      materializeAnyDiffable(c)
    }

    q"""new _root_.org.backuity.matchete.Diffable[$tag] {
            import _root_.org.backuity.matchete.Diffable
            import Diffable.{DiffResult,Equal,NestedDiff,SomeDiff,BasicDiff}
            def diff(a: $tag, b: $tag) : DiffResult = {
              $diffLogic
            }
          }
     """
  }

  def materializeCaseClassDiffable[T: c.WeakTypeTag](c : blackbox.Context)(tag: c.WeakTypeTag[T]) : c.Tree = {
    import c.universe._

    val caseAttributes = tag.tpe.members.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }.toList

    val implicits = caseAttributes.map { ca =>
      q"""implicitly[Diffable[${ca.typeSignature.resultType}]].diff(a.$ca, b.$ca) match {
               case Equal => // OK
               case diff : SomeDiff =>
                  return NestedDiff(a,b,${ca.name.toString},diff)
            }
         """
    }

    q"""..$implicits
          Equal
       """
  }

  def materializeSeqDiffable[T: c.WeakTypeTag](c : blackbox.Context)(tag: c.WeakTypeTag[T]) : c.Tree = {
    import c.universe._
    // uses the index as the path element
    val elementType = tag.tpe.typeArgs.head
    q"""
        if( a.size != b.size ) {
          NestedDiff(a,b,"size",BasicDiff(a.size, b.size))
        } else {
          for( i <- 0 until a.size ) {
            implicitly[Diffable[$elementType]].diff(a(i), b(i)) match {
              case Equal => // OKAY
              case diff : SomeDiff =>
                return NestedDiff(a,b,"(" + i + ")",diff)
            }
          }
          Equal
        }
    """
  }

  def materializeSetDiffable[T: c.WeakTypeTag](c : blackbox.Context)(tag: c.WeakTypeTag[T]) : c.Tree = {
    import c.universe._
    val elementType = tag.tpe.typeArgs.head
    q"""
        val formatter = implicitly[Formatter[$elementType]]
        val missingElements = b -- a
        var reasons : List[String] = Nil
        if( missingElements.nonEmpty ) {
          reasons = ("missing elements: " + formatter.formatAll(missingElements)) :: reasons
        }

        val extraElements = a -- b
        if( extraElements.nonEmpty ) {
          reasons = ("extra elements: " + formatter.formatAll(extraElements)) :: reasons
        }

        if( reasons.isEmpty ) {
          Equal
        } else {
          BasicDiff(a,b,reasons)
        }
    """
  }

  def materializeMapDiffable[T: c.WeakTypeTag](c : blackbox.Context)(tag: c.WeakTypeTag[T]) : c.Tree = {
    import c.universe._
    val valueType : c.Type = tag.tpe.typeArgs(1)
    q"""
        if( a != b ) {
          val missingKeys = (a.keySet -- b.keySet) ++ (b.keySet -- a.keySet)
          missingKeys.headOption match {
            case Some(missingKey) =>
              NestedDiff(a,b,"get(" + missingKey + ")",BasicDiff(a.get(missingKey),b.get(missingKey)))

            case None =>
              (a.find {
                case (k,v) => b(k) != v
              }) match {
                case Some((k,v)) =>
                  implicitly[Diffable[$valueType]].diff(a(k),b(k)) match {
                    case Equal =>
                      BasicDiff(a,b) // shouldn't happen

                    case diff : SomeDiff =>
                      NestedDiff(a,b,"get(" + k + ")",diff)
                  }

                case None => BasicDiff(a,b)
              }
          }
        } else {
          Equal
        }
     """
  }

  def materializeAnyDiffable[T: c.WeakTypeTag](c : blackbox.Context) : c.Tree = {
    import c.universe._
    q"""if( a != b ) {
          BasicDiff(a, b)
        } else {
          Equal
        }
    """
  }

}
