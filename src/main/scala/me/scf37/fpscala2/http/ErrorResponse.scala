package me.scf37.fpscala2.http
import tethys.derivation.semiauto._

case class ErrorResponse(errors: Seq[String])

object ErrorResponse {
  implicit val writer = jsonWriter[ErrorResponse]
}
