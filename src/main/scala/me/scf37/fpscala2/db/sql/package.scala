package me.scf37.fpscala2.db

import java.sql.Connection

import cats.data.Kleisli
import cats.effect.Sync

package object sql {
 type SqlDb[F[_], A] = Kleisli[F, Connection, A]

  implicit def db[F[_]: Sync]: Db[SqlDb[F, ?], F] = new Db[SqlDb[F, ?], F] {
    override def lift[A](f: Connection => F[A]): SqlDb[F, A] = Kleisli.apply(f)
  }

  implicit def dbEval[F[_]: Sync]: DbEval[SqlDb[F, ?], F] = new DbEval[SqlDb[F, ?], F] {
    override def eval[A](f: SqlDb[F, A], c: Connection): F[A] = f(c)
  }
}
