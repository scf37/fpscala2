package me.scf37.fpscala2.db

import java.sql.Connection


/**
  * Typeclass for database effect.
  *
  * Lifts JDBC code using provided Connection to F
  */
trait Db[F[_]] {
  def eval[T](f: Connection => T): F[T]
}

object Db {
  def apply[F[_]: Db] = implicitly[Db[F]]
}

