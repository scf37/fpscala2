package me.scf37.fpscala2.db

import cats.~>

/**
  * Convert database effect to generic effect
  *
  * @tparam F generic effect
  * @tparam DbEffect database effect
  */
trait TxManager[F[_], DbEffect[_]] {
  def tx: DbEffect ~> F
}