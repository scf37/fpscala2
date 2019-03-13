package me.scf37.fpscala2.logging

import com.twitter.finagle.http.Response

trait Log[F[_]] {

  def logInfo(msg: => String): F[Unit]

  def logRequest(msg: => String): F[Unit]

  def logValidationError(msg: => String, ex: Throwable): F[Unit]

  def logUnexpectedError(msg: => String, ex: Throwable): F[Unit]
}
