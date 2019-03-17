package me.scf37.fpscala2.db

import java.sql.Connection

import cats.data.Kleisli
import cats.effect.Sync

package object sql {
  /**
    * JDBC-specific implementation of DbEffect. It is a function that takes Connection and returns
    * result from the database.
    *
    * @tparam F generic effect used inside DAO layer
    * @tparam A type of result
    */
 type SqlEffect[F[_], A] = Kleisli[F, Connection, A]

  implicit def db[F[_]: Sync]: SqlEffectLift[F, SqlEffect[F, ?]] = new SqlEffectLift[F, SqlEffect[F, ?]] {
    override def lift[A](f: Connection => F[A]): SqlEffect[F, A] = Kleisli.apply(f)
  }

  implicit def dbEval[F[_]: Sync]: SqlEffectEval[F, SqlEffect[F, ?]] = new SqlEffectEval[F, SqlEffect[F, ?]] {
    override def eval[A](f: SqlEffect[F, A], c: Connection): F[A] = f(c)
  }
}
