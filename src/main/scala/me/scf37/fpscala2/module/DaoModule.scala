package me.scf37.fpscala2.module

import cats.Monad
import cats.MonadError
import cats.effect.Sync
import me.scf37.fpscala2.dao.TodoDao
import me.scf37.fpscala2.dao.sql.TodoDaoSql
import me.scf37.fpscala2.db.sql.SqlEffectLift

case class DaoModule[I[_], F[_]](
  todoDao: I[TodoDao[F]]
)

object DaoModule {

  def apply[I[_]: Later: Monad, F[_]](
    implicit DB: SqlEffectLift[F],
    ME: MonadError[F, Throwable]
  ): DaoModule[I, F] = DaoModule[I, F](
    todoDao = Later[I].later {
      new TodoDaoSql[F]
    }
  )
}
