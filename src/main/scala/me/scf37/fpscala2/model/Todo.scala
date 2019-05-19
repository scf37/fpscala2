package me.scf37.fpscala2.model

import tethys.derivation.builder.ReaderDerivationConfig
import tethys.derivation.semiauto._

case class Todo(
  id: String,
  text: String
)

object Todo {
  implicit val reader = jsonReader[Todo](ReaderDerivationConfig.strict)
  implicit val writer = jsonWriter[Todo]
}