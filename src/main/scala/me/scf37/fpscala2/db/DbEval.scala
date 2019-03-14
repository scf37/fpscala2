package me.scf37.fpscala2.db

import java.sql.Connection


/**
  * Typeclass for evaluation of database effect.
  *
  */
trait DbEval[DbEffect[_], F[_]] {
  /**
    * Evaluate database effect f using provided JDBC connection
    *
    * @param f database effect
    * @param c jdbc connection
    * @tparam A result type
    * @return generic effect for evaluated A
    */
  def eval[A](f: DbEffect[A], c: Connection): F[A]
}

object DbEval {
  def apply[DbEffect[_], F[_]](implicit DE: DbEval[DbEffect, F]): DbEval[DbEffect, F] = DE
}


