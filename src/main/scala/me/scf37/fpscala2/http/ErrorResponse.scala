package me.scf37.fpscala2.http
import me.scf37.fpscala2.tethys.JsonWriter3

case class ErrorResponse(errors: Seq[String]) derives JsonWriter3
