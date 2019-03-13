package me.scf37.fpscala2.db

import cats.~>

/**
  * Convert database effect to generic effect
  *
  * @tparam F generic effect
  * @tparam T database effect
  */
trait TxManager[F[_], T[_]] {
  def tx: T ~> F
}