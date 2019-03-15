package me.scf37.fpscala2.exception

import scala.util.control.NoStackTrace

abstract class RestException(val errors: Seq[String], val status: Int)
  extends RuntimeException(errors.mkString(", ")) with NoStackTrace

case class ResourceAlreadyExistsException(msg: String) extends RestException(Seq(msg), 400)
case class ResourceNotFoundException(msg: String) extends RestException(Seq(msg), 404)

case class ValidationFailedException(errors1: Seq[String]) extends RestException(errors1, 400)
