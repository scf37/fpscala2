package me.scf37.fpscala2.exception

import scala.util.control.NoStackTrace

/**
  * Handled by ExceptionFilter in predictable way, resulting in appropriate json error message
  */
sealed trait RestException extends RuntimeException with NoStackTrace:
  def errors: Seq[String]
  def status: Int


abstract class RestExceptionImpl private[exception] (val errors: Seq[String], val status: Int)
  extends RuntimeException(errors.mkString(", "))

final case class ResourceAlreadyExistsException(msg: String)
  extends RestExceptionImpl(Seq(msg), 400) with RestException

final case class ResourceNotFoundException(msg: String)
  extends RestExceptionImpl(Seq(msg), 404) with RestException

final case class ValidationFailedException(errors1: Seq[String])
  extends RestExceptionImpl(errors1, 400) with RestException
