package tethys.readers

import me.scf37.fpscala2.tethys.JsonReader3
import tethys.JsonReader
import tethys.readers.instances.SimpleJsonReader
import tethys.readers.instances.SimpleJsonReader.FieldDefinition
import tethys.readers.tokens.TokenIterator

import scala.compiletime.ops
import scala.deriving._
import scala.compiletime._

// SimpleJsonReader is package-private, JsonReaderBuilder can not be assembled by inline methods and
// reimplementing it adds too much specific code.
object JsonReaderInternal {
  inline def productReader[A, Elems <: Tuple](p: Mirror.Product): JsonReader3[A] = {
    val reader = new SimpleJsonReader[A](collectFieldDefinitions[Elems].toArray, arr => p.fromProduct(ProductArray(arr)).asInstanceOf[A], strict = true)
    new JsonReader3[A] {
      override def read(it: TokenIterator)(implicit fieldName: FieldName): A = reader.read(it)
    }
  }

  inline def collectFieldDefinitions[Elems <: Tuple]: List[FieldDefinition[_]] = inline erasedValue[Elems] match {
    case _: EmptyTuple => Nil
    case _: ((tt, tn) *: ts) =>
      FieldDefinition[Any](constValue[tn].toString, defaultValue[tt] , summonInline[JsonReader[tt]].asInstanceOf[JsonReader[Any]]) :: collectFieldDefinitions[ts]
  }

  // unfortunately JsonReaderDefaultValue does not work, see https://github.com/lampepfl/dotty/issues/12109
  inline def defaultValue[A]: Any = inline erasedValue[A] match {
    case _: Option[_] => None
    case _ => null
  }

  private case class ProductArray(arr: Array[_]) extends Product {
    override def productArity: Int = arr.length
    override def productElement(n: Int): Any = arr(n)
  }
}
