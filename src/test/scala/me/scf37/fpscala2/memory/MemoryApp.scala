package me.scf37.fpscala2.memory

import cats.Monad
import cats.arrow.FunctionK
import cats.effect.Effect
import cats.effect.IO
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

class MemoryApp[I[_]: Later: Monad, F[_]: Effect] extends Application[I, F](ApplicationConfig.testConfig) {
  import me.scf37.fpscala2.db.sql.db

  override lazy val daoModule: DaoModule[SqlDbF, I] = new DaoModuleImpl[SqlDbF, I] {
    override lazy val todoDao: I[TodoDao[SqlDbF]] = Later[I].later {
      new MemoryTodoDao[SqlDbF]
    }
  }

  override lazy val dbModule: DbModule[F, SqlDbF, I] = new DbModule[F, SqlDbF, I] {
    override lazy val tx: I[TxManager[F, SqlDbF]] = Later[I].later {
      new TxManager[F, SqlDbF] {
        override def tx: SqlDbF ~> F = FunctionK.lift(impl)

        private def impl[A](t: SqlDb[F, A]): F[A] = t.apply(null)
      }
    }
  }
}
