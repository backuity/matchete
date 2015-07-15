/*
 * Copyright 2013 Bruno Bieth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.backuity.matchete

import org.backuity.matchete.Diffable.{BasicDiff, NestedDiff, SomeDiff}

import scala.collection.{SortedSet, SortedMap}

trait Formatter[-T] {
  def format(t : T) : String
}

object Formatter extends UnorderedMapFormatter {
  def apply[T](f : T => String) : Formatter[T] = new Formatter[T] {
    def format(t: T ) : String = f(t)
  }

  implicit val stringFormatter : Formatter[String] = Formatter[String]{ s => s"'$s'" }

  implicit def arrayFormatter[T] : Formatter[Array[T]] = Formatter[Array[T]]{ _.deep.toString }

  implicit def orderedMapFormatter[K : Formatter : Ordering,V : Formatter] : Formatter[Map[K,V]] = new Formatter[Map[K, V]] {
    override def format(t: Map[K, V]): String = {
      val tupleFormatter : Formatter[(K,V)] = Formatter[(K,V)]( kv => formatI(kv._1) + " -> " + formatI(kv._2))
      traversableFormatter[(K,V)](tupleFormatter).format(SortedMap(t.toSeq : _*))
    }
  }

//  implicit def orderedSetFormatter[T : Formatter : Ordering] : Formatter[Set[T]] = new Formatter[Set[T]] {
//    override def format(t: Set[T]): String = {
//      traversableFormatter[T].format(SortedSet(t.toSeq : _*))
//    }
//  }

  implicit def diffFormatter[T](implicit formatter: Formatter[T]) : Formatter[SomeDiff[T]] = new Formatter[SomeDiff[T]] {
    override def format(diff: SomeDiff[T]): String = {

      val formattedArgA = formatter.format(diff.sourceA)
      val formattedArgB = formatter.format(diff.sourceB)

      val widthA = Formatter.width(formattedArgA)
      val widthB = Formatter.width(formattedArgB)

      val formattedArguments = if( (widthA + widthB) > 80 ) {
        s"\n${indent(formattedArgA, 2)}\n\nis not equal to\n\n${indent(formattedArgB,2)}\n"
      } else {
        s"$formattedArgA is not equal to $formattedArgB"
      }

      val reasonsString = if( diff.reasons.isEmpty ) "" else {
        "\nReasons:\n * " + diff.reasons.map( r => indent(r, 3, firstLine = false)).mkString("\n * ")
      }

      diff match {
        case _ : BasicDiff[T] =>
          s"$formattedArguments$reasonsString"

        case nestedDiff : NestedDiff[T] =>
          val indentLevel = 10 + nestedDiff.path.length + 3

          s"""$formattedArguments
             |Got     : ${indent(nestedDiff.pathValueA,indentLevel,firstLine = false)}
             |Expected: ${indent(nestedDiff.pathValueB,indentLevel,firstLine = false)}$reasonsString""".stripMargin
      }
    }
  }

  implicit def tuple2Formatter[A : Formatter,B : Formatter] : Formatter[(A,B)] = new Formatter[(A, B)] {
    override def format(t: (A, B)): String = s"(${formatI(t._1)},${formatI(t._2)})"
    override def toString = "tuple2"
  }
  implicit def tuple3Formatter[A : Formatter,B : Formatter,C : Formatter] : Formatter[(A,B,C)] = new Formatter[(A,B,C)] {
    override def format(t: (A,B,C)): String = s"(${formatI(t._1)},${formatI(t._2)},${formatI(t._3)})"
  }
  implicit def tuple4Formatter[A : Formatter,B : Formatter,C : Formatter,D : Formatter] : Formatter[(A,B,C,D)] = new Formatter[(A,B,C,D)] {
    override def format(t: (A,B,C,D)): String = s"(${formatI(t._1)},${formatI(t._2)},${formatI(t._3)},${formatI(t._4)})"
  }

  /** format implicitly */
  @inline def formatI[T : Formatter](t : T) : String = implicitly[Formatter[T]].format(t)

  def width(string: String) : Int = {
    string.split('\n').map(_.length).max
  }

  def indent(str: String, space: Int, firstLine: Boolean = true) : String = {
    val spaces = " " * space
    (if( firstLine ) spaces else "") + str.split('\n').mkString("\n" + spaces)
  }
}

trait UnorderedMapFormatter extends FormatterLowImplicits {

  import Formatter.formatI

  // ambiguous with orderedMapFormatter
  implicit def mapFormatter[K : Formatter,V : Formatter] : Formatter[Map[K,V]] = new Formatter[Map[K, V]] {
    override def format(t: Map[K, V]): String = {
      val tupleFormatter : Formatter[(K,V)] = Formatter[(K,V)]( kv => formatI(kv._1) + " -> " + formatI(kv._2))
      traversableFormatter[(K,V)](tupleFormatter).format(t)
    }
  }
}

trait FormatterLowImplicits {

  import Formatter.indent

  /** print 'null' if the value is null */
  implicit def anyFormatter[T] : Formatter[T] = Formatter[T]{ _.toString }

  def traversableContentFormatter[T : Formatter] : Formatter[Traversable[T]] = new TraversableFormatter[T](printCollectionName = false)

  // ambiguous with mapFormatter
  implicit def traversableFormatter[T : Formatter] : Formatter[Traversable[T]] = new TraversableFormatter[T](printCollectionName = true)

  class TraversableFormatter[T](printCollectionName: Boolean)(implicit formatter: Formatter[T]) extends Formatter[Traversable[T]] {
    override def format(t: Traversable[T]): String = {
      val formattedElements = t.map(elem => formatter.format(elem))
      val formattedElementsString = formattedElements.mkString(", ")
      val draft = if( printCollectionName ) {
        s"${t.stringPrefix}($formattedElementsString)"
      } else {
        formattedElementsString
      }

      if( Formatter.width(draft) < 100 ) {
        draft
      } else {
        // multiline display
        val formattedElementsString = formattedElements.mkString(",\n")
        val oneElementPerLine = if( printCollectionName ) {
          t.stringPrefix + "(\n" + indent(formattedElementsString, 2) + ")"
        } else {
          formattedElementsString
        }
        oneElementPerLine
      }
    }
  }
}