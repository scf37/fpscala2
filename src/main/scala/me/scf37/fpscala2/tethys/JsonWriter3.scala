package me.scf37.fpscala2.tethys

import me.scf37.fpscala2.model.Todo
import tethys.JsonObjectWriter
import tethys.JsonReader
import tethys.JsonWriter
import tethys.writers.tokens.TokenWriter

import scala.deriving._
import scala.compiletime._

/**
  * Scala3 derivation for JsonWriter
  * @tparam A
  */
trait JsonWriter3[A] extends JsonWriter[A]

object JsonWriter3 {
  inline def derived[A](using m: Mirror.Of[A]): JsonWriter3[A] =
    inline m match {
      case p: Mirror.ProductOf[A] => productWriter[A, Tuple.Zip[p.MirroredElemTypes, p.MirroredElemLabels]]
      case s: Mirror.SumOf[A] => throw new IllegalStateException("sums are not supported")
    }

  private inline def productWriter[A, Elems <: Tuple]: JsonWriter3[A] = {
    new JsonObjectWriter[A] with JsonWriter3[A] {
      override def writeValues(value: A, tokenWriter: TokenWriter): Unit = writeAll[Elems, 0](value.asInstanceOf[Product], tokenWriter)
    }
  }

  inline def writeAll[Elems <: Tuple, Ix <: Int](value: Product, tokenWriter: TokenWriter): Unit = inline erasedValue[Elems] match {
    case _: EmptyTuple =>
    case _: ((tt, tn) *: ts) =>
      summonInline[JsonWriter[tt]].asInstanceOf[JsonWriter[Any]].write(constValue[tn].toString, value.productElement(constValue[Ix]), tokenWriter)
      writeAll[ts, ops.int.S[Ix]](value, tokenWriter)
  }

}
