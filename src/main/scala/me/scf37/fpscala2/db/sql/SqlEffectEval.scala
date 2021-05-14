package me.scf37.fpscala2.db.sql

import me.scf37.fpscala2.typeclass.Run

import java.sql.Connection


/**
  * Typeclass for evaluation of database effect.
  *
  */
trait SqlEffectEval[F[_], DbEffect[_]]:
  /**
    * Evaluate database effect f using provided JDBC connection
    *
    * @param f database effect
    * @param c jdbc connection
    * @tparam A result type
    * @return generic effect for evaluated A
    */
  def eval[A](f: DbEffect[A], c: Connection): F[A]


object SqlEffectEval:
  def apply[DbEffect[_], F[_]](using DE: SqlEffectEval[DbEffect, F]): SqlEffectEval[DbEffect, F] = DE

  given dbEval[F[_], SqlEffect[_]](
    using R: Run[SqlEffect, F, Connection]
  ): SqlEffectEval[F, SqlEffect] = new SqlEffectEval[F, SqlEffect]:
    override def eval[A](f: SqlEffect[A], c: Connection): F[A] = R.run(f)(c)
