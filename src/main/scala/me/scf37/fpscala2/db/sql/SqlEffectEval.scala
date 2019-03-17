package me.scf37.fpscala2.db.sql

import java.sql.Connection


/**
  * Typeclass for evaluation of database effect.
  *
  */
trait SqlEffectEval[DbEffect[_], F[_]] {
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

object SqlEffectEval {
  def apply[DbEffect[_], F[_]](implicit DE: SqlEffectEval[DbEffect, F]): SqlEffectEval[DbEffect, F] = DE
}


