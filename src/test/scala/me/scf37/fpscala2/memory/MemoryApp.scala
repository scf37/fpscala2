package me.scf37.fpscala2.memory

import java.sql.Connection

import cats.Monad
import cats.effect.Effect
import me.scf37.fpscala2.Application
import me.scf37.fpscala2.config.ApplicationConfig
import me.scf37.fpscala2.config.Later
import me.scf37.fpscala2.config.module.DaoModule
import me.scf37.fpscala2.config.module.DbModule
import me.scf37.fpscala2.db.Db
import me.scf37.fpscala2.db.DbEval
import me.scf37.fpscala2.memory.MemoryApp._

class MemoryApp[I[_]: Later: Monad, F[_]: Effect](
) extends Application[I, F, F](ApplicationConfig.testConfig) {

  override lazy val daoModule: DaoModule[F, I] = new MemoryDaoModule[F, I]

  override lazy val dbModule: DbModule[F, F, I] = new MemoryDbModule[F, I]
}

object MemoryApp {
  implicit def DB[F[_]]: Db[F, F] = new Db[F, F] {
    override def lift[A](f: Connection => F[A]): F[A] = ???
  }

  implicit def DE[F[_]]: DbEval[F, F] = new DbEval[F, F] {
    override def eval[A](f: F[A], c: Connection): F[A] = ???
  }

}
