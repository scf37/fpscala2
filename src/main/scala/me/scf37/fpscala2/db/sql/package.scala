package me.scf37.fpscala2.db

import java.sql.Connection

import cats.data.Kleisli
import cats.effect.Sync

package object sql {
 type SqlEffect[F[_], A] = Kleisli[F, Connection, A]

  implicit def db[F[_]: Sync]: SqlEffectLift[SqlEffect[F, ?], F] = new SqlEffectLift[SqlEffect[F, ?], F] {
    override def lift[A](f: Connection => F[A]): SqlEffect[F, A] = Kleisli.apply(f)
  }

  implicit def dbEval[F[_]: Sync]: SqlEffectEval[SqlEffect[F, ?], F] = new SqlEffectEval[SqlEffect[F, ?], F] {
    override def eval[A](f: SqlEffect[F, A], c: Connection): F[A] = f(c)
  }
}
