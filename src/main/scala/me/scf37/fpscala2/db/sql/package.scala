package me.scf37.fpscala2.db

import java.sql.Connection

import cats.data.Kleisli
import cats.effect.Sync

package object sql {
 type SqlDb[F[_], A] = Kleisli[F, Connection, A]

  implicit def db[F[_]: Sync]: Db[SqlDb[F, ?]] = new Db[SqlDb[F, ?]] {
    override def eval[T](f: Connection => T): SqlDb[F, T] = Kleisli.apply(c => Sync[F].delay(f(c)))
  }
}

package object sql2 {
  case class SqlDb[F[_], A](f: Connection => F[A])
}
