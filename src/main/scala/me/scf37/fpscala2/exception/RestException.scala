package me.scf37.fpscala2.exception

import scala.util.control.NoStackTrace

abstract class RestException(msg: String, status: Int) extends RuntimeException(msg) with NoStackTrace

case class ResourceAlreadyExistsException(msg: String) extends RestException(msg, 400)
case class ResourceNotFoundException(msg: String) extends RestException(msg, 400)
