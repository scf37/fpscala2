package me.scf37.fpscala2.tethys

import tethys.JsonReader
import tethys.readers.JsonReaderBuilder.JsonReaderBuilder1
import tethys.readers.JsonReaderInternal

import scala.deriving._
import scala.compiletime._

/**
  * Scala3 derivation for JsonReader
  * @tparam A
  */
trait JsonReader3[A] extends JsonReader[A]

object JsonReader3 {
  inline def derived[A](using m: Mirror.Of[A]): JsonReader3[A] =
    inline m match {
      case p: Mirror.ProductOf[A] => JsonReaderInternal.productReader[A, Tuple.Zip[p.MirroredElemTypes, p.MirroredElemLabels]](p)
      case s: Mirror.SumOf[A] => throw new IllegalStateException("sums are not supported")
    }

}
