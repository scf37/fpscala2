package me.scf37.fpscala2.module

import cats.Monad
import cats.MonadError
import cats.effect.Sync
import me.scf37.fpscala2.dao.TodoDao
import me.scf37.fpscala2.dao.sql.TodoDaoSql
import me.scf37.fpscala2.db.sql.SqlEffectLift
import me.scf37.fpscala2.module.init.Init

case class DaoModule[I[_], F[_]](
  todoDao: I[TodoDao[F]]
):
  def mapK[II[_]](f: [A] => I[A] => II[A]): DaoModule[II, F] = DaoModule(
    todoDao = f(todoDao)
  )


object DaoModule:

  def apply[I[_]: Monad, F[_], DbEffect[_]](
    using DB: SqlEffectLift[DbEffect],
    ME: MonadError[DbEffect, Throwable],
    init: Init[I, F]
  ): DaoModule[I, DbEffect] = DaoModule[I, DbEffect](
    todoDao = init delay new TodoDaoSql[DbEffect]
  )
