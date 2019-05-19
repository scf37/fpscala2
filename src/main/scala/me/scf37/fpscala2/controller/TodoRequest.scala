package me.scf37.fpscala2.controller

import java.util.regex.Pattern

import cats.data.ValidatedNel
import cats.implicits._
import tethys.derivation.semiauto._

case class TodoRequest(
  text: String
) {
  import TodoRequest._

  def validate(id: String): ValidatedNel[String, TodoRequest] =
    List(
      validateNonEmpty(id, "id"),
      validateMaxSize(id, "id", 100),
      validateRegex(id, "id", idRegex, "must only contain alphanumeric characters or underscore"),

      validateMaxSize(text, "text", 100)

    ).sequence.map(_ => this)

}

object TodoRequest {
  implicit val reader = jsonReader[TodoRequest]

  private val idRegex = Pattern.compile("[\\w]+")

  private def validateMaxSize(s: String, field: String, max: Int): ValidatedNel[String, Unit] =
    if (s.length > max)
      s"$field: length is not less than or equal $max".invalidNel
    else
      ().validNel

  private def validateNonEmpty(s: String, field: String): ValidatedNel[String, Unit] =
    if (s.isEmpty)
      s"$field: cannot be empty".invalidNel
    else
      ().validNel

  private def validateRegex(s: String, field: String, regex: Pattern, msg: String): ValidatedNel[String, Unit] =
    if (!regex.matcher(s).matches())
      s"$field: $msg".invalidNel
    else
      ().validNel
}
