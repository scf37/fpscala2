package me.scf37.fpscala2.memory

import cats.Monad
import cats.arrow.FunctionK
import cats.effect.IO
import cats.effect.Sync
import cats.~>
import me.scf37.fpscala2.Application
import me.scf37.fpscala2.config.ApplicationConfig
import me.scf37.fpscala2.config.Later
import me.scf37.fpscala2.config.module.DaoModule
import me.scf37.fpscala2.config.module.DaoModuleImpl
import me.scf37.fpscala2.config.module.DbModule
import me.scf37.fpscala2.dao.TodoDao
import me.scf37.fpscala2.db.TxManager
import me.scf37.fpscala2.db.sql.SqlDb

class MemoryApp[I[_]: Later: Monad] extends Application[I](ApplicationConfig.testConfig) {

  override lazy val daoModule: DaoModule[SqlDb, I] = new DaoModuleImpl[SqlDb, I] {
    override lazy val todoDao: I[TodoDao[SqlDb]] = Later[I].later {
      new MemoryTodoDao[SqlDb]
    }
  }

  override lazy val dbModule: DbModule[IO, SqlDb, I] = new DbModule[IO, SqlDb, I] {
    override lazy val tx: I[TxManager[IO, SqlDb]] = Later[I].later {
      new TxManager[IO, SqlDb] {
        override def tx: SqlDb ~> IO = FunctionK.lift(impl)

        private def impl[A](t: SqlDb[A]): IO[A] = Sync[IO].fromEither(t.f(null))
      }
    }
  }
}
