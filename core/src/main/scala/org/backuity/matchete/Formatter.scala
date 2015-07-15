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

import org.backuity.matchete.Diffable.{NestedDiff, BasicDiff, SomeDiff}

import scala.collection.SortedMap

trait Formatter[-T] {
  def format(t : T) : String

  def formatAll(elems: Traversable[T]) : String = elems.map(format).mkString(", ")
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
        ":\n * " + diff.reasons.mkString("\n * ")
      }

      diff match {
        case _ : BasicDiff[T] => s"$formattedArguments$reasonsString"
        case nestedDiff : NestedDiff[T] => s"$formattedArguments\n" +
            s"Got     : ${nestedDiff.pathValueA}\n" +
            s"Expected: ${nestedDiff.pathValueB}$reasonsString"
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

  def indent(str: String, space: Int) : String = {
    val spaces = " " * space
    str.split('\n').map( s => spaces + s ).mkString("\n")
  }
}

trait UnorderedMapFormatter extends FormatterLowImplicits {

  import Formatter.formatI

  implicit def mapFormatter[K : Formatter,V : Formatter] : Formatter[Map[K,V]] = new Formatter[Map[K, V]] {
    override def format(t: Map[K, V]): String = {
      val tupleFormatter : Formatter[(K,V)] = Formatter[(K,V)]( kv => formatI(kv._1) + " -> " + formatI(kv._2))
      traversableFormatter[(K,V)](tupleFormatter).format(t)
    }
  }
}

trait FormatterLowImplicits {

  /** print 'null' if the value is null */
  implicit def anyFormatter[T] : Formatter[T] = Formatter[T]{ _.toString }

  implicit def traversableFormatter[T](implicit formatter: Formatter[T]) : Formatter[Traversable[T]] = new Formatter[Traversable[T]] {
    def format(t: Traversable[T]): String = {
      val formattedElements = t.map(elem => formatter.format(elem))
      val draft = t.stringPrefix + "(" + formattedElements.mkString(", ") + ")"
      if( Formatter.width(draft) < 100 ) {
        draft
      } else {
        val oneElementPerLine = t.stringPrefix + "(\n  " + formattedElements.mkString(",\n  ") + ")"
        oneElementPerLine
      }
    }
  }
}