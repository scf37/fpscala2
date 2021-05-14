package me.scf37.fpscala2.model

import me.scf37.fpscala2.tethys.JsonReader3
import me.scf37.fpscala2.tethys.JsonWriter3
import tethys.JsonReader
import tethys.JsonWriter

case class Todo(
  id: String,
  text: String
) derives JsonWriter3, JsonReader3
