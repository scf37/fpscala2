package me.scf37.fpscala2.http

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response

trait Filter[F[_]] extends ((Request => F[Response]) => (Request => F[Response])) {
  override def apply(orig: Request => F[Response]): Request => F[Response]
}
