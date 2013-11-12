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