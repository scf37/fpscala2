package me.scf37.fpscala2.db

import cats.~>

/**
  * Transaction manager, opens transactions, executes transactional code, commits or rollbacks transaction.
  *
  * It can be seen as a function that takes DbEffect, evaluates it using jdbc Connection and wraps
  * everything into generic effect.
  *
  * @tparam F generic effect
  * @tparam DbEffect database effect
  */
trait TxManager[F[_], DbEffect[_]] {
  def tx: DbEffect ~> F
}