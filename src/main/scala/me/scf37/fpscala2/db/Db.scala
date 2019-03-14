package me.scf37.fpscala2.db

import java.sql.Connection


/**
  * Typeclass for lifting JDBC code into database effect.
  *
  * Database effect required java.sql.Connection instance for evaluation.
  */
trait Db[DbEffect[_], F[_]] {
  def lift[A](f: Connection => F[A]): DbEffect[A]
}

object Db {
  def apply[DbEffect[_], F[_]](implicit db: Db[F, DbEffect]): Db[F, DbEffect] = db
}

