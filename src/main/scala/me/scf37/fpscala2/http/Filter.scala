package me.scf37.fpscala2.http

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response

/**
  * HTTP service filter, i.e. function that converts HTTP service (Request => F[Response])
  * to another service of the same type
  *
  * @tparam F generic effect type
  */
trait Filter[F[_]] extends ((Request => F[Response]) => (Request => F[Response])):
  override def apply(orig: Request => F[Response]): Request => F[Response]

