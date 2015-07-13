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
    def valueA : Any
    def valueB : Any

    /** @return a readable string containing the path of the source of the diff along with the value of the first element */
    def pathValueA : String = formatPathWithValue(valueA)

    /** @return a readable string containing the path of the source of the diff along with the value of the second element */
    def pathValueB : String = formatPathWithValue(valueB)

    def path : String = path0("")

    private[Diffable] def path0(prefix: String) : String

    def reasons: List[String]

    private def formatPathWithValue(value: Any) : String = {
      if( path.isEmpty ) value.toString else path + " = " + value
    }
  }
  case class BasicDiff(sourceA: Any, sourceB: Any, reasons : List[String] = Nil) extends SomeDiff {
    def valueA = sourceA
    def valueB = sourceB
    private[Diffable] def path0(prefix: String) : String = prefix
  }
  case class NestedDiff(sourceA: Any, sourceB: Any, origin: String, detail: SomeDiff) extends SomeDiff {
    def valueA = detail.valueA
    def valueB = detail.valueB
    private[Diffable] def path0(prefix: String) : String = {
      val newPrefix = if( prefix.isEmpty ) origin else {
        prefix + {if( origin.isEmpty ) "" else { "." + origin }}
      }
      detail.path0(newPrefix)
    }
    def reasons = detail.reasons
  }

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
      q"""implicitly[_root_.org.backuity.matchete.Diffable[${ca.typeSignature.resultType.asSeenFrom(tag.tpe,tag.tpe.typeSymbol)}]].diff(a.$ca, b.$ca) match {
               case _root_.org.backuity.matchete.Diffable.Equal => // OK
               case diff : _root_.org.backuity.matchete.Diffable.SomeDiff =>
                  return _root_.org.backuity.matchete.Diffable.NestedDiff(a,b,${ca.name.toString},diff)
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
          _root_.org.backuity.matchete.Diffable.NestedDiff(a,b,"size",
            _root_.org.backuity.matchete.Diffable.BasicDiff(a.size, b.size))
        } else {
          for( i <- 0 until a.size ) {
            implicitly[_root_.org.backuity.matchete.Diffable[$elementType]].diff(a(i), b(i)) match {
              case _root_.org.backuity.matchete.Diffable.Equal => // OKAY

              case diff : _root_.org.backuity.matchete.Diffable.SomeDiff =>
                return _root_.org.backuity.matchete.Diffable.NestedDiff(a,b,"(" + i + ")",diff)
            }
          }
          _root_.org.backuity.matchete.Diffable.Equal
        }
    """
  }

  def materializeSetDiffable[T: c.WeakTypeTag](c : blackbox.Context)(tag: c.WeakTypeTag[T]) : c.Tree = {
    import c.universe._
    val elementType = tag.tpe.typeArgs.head
    q"""
        val formatter = implicitly[_root_.org.backuity.matchete.Formatter[$elementType]]
        val missingElements = b -- a
        val extraElements = a -- b

        if( missingElements.isEmpty && extraElements.isEmpty ) {

          _root_.org.backuity.matchete.Diffable.Equal

        } else if( missingElements.size == 1 && extraElements.size == 1 ) {

          // special case, we'll diff that one element
          implicitly[_root_.org.backuity.matchete.Diffable[$elementType]].diff(extraElements.head, missingElements.head) match {
            case _root_.org.backuity.matchete.Diffable.Equal =>
              _root_.org.backuity.matchete.Diffable.BasicDiff(a,b) // heck they are different!

            case someDiff : _root_.org.backuity.matchete.Diffable.SomeDiff =>
              _root_.org.backuity.matchete.Diffable.NestedDiff(a,b,"<some-element>",someDiff)
          }

        } else {
          var reasons : _root_.scala.collection.immutable.List[_root_.java.lang.String] = _root_.scala.collection.immutable.Nil
          if( missingElements.nonEmpty ) {
            reasons = ("missing elements: " + formatter.formatAll(missingElements)) :: reasons
          }

          if( extraElements.nonEmpty ) {
            reasons = ("extra elements: " + formatter.formatAll(extraElements)) :: reasons
          }

          _root_.org.backuity.matchete.Diffable.BasicDiff(a,b,reasons)
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
              _root_.org.backuity.matchete.Diffable.NestedDiff(a,b,"get(" + missingKey + ")",
                _root_.org.backuity.matchete.Diffable.BasicDiff(a.get(missingKey),b.get(missingKey)))

            case None =>
              (a.find {
                case (k,v) => b(k) != v
              }) match {
                case Some((k,v)) =>
                  implicitly[_root_.org.backuity.matchete.Diffable[$valueType]].diff(a(k),b(k)) match {
                    case Equal =>
                      _root_.org.backuity.matchete.Diffable.BasicDiff(a,b) // shouldn't happen

                    case diff : SomeDiff =>
                      _root_.org.backuity.matchete.Diffable.NestedDiff(a,b,"get(" + k + ")",diff)
                  }

                case None => _root_.org.backuity.matchete.Diffable.BasicDiff(a,b)
              }
          }
        } else {
          _root_.org.backuity.matchete.Diffable.Equal
        }
     """
  }

  def materializeAnyDiffable[T: c.WeakTypeTag](c : blackbox.Context) : c.Tree = {
    import c.universe._
    q"""if( a != b ) {
          _root_.org.backuity.matchete.Diffable.BasicDiff(a, b)
        } else {
          _root_.org.backuity.matchete.Diffable.Equal
        }
    """
  }

}
