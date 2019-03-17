package me.scf37.fpscala2.db.sql

import java.sql.Connection


/**
  * Typeclass for lifting JDBC code into database effect.
  *
  * Database effect required java.sql.Connection instance for evaluation.
  */
trait SqlEffectLift[DbEffect[_], F[_]] {
  def lift[A](f: Connection => F[A]): DbEffect[A]
}

object SqlEffectLift {
  def apply[DbEffect[_], F[_]](implicit db: SqlEffectLift[F, DbEffect]): SqlEffectLift[F, DbEffect] = db
}

