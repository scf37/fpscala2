package me.scf37.fpscala2.memory

import cats.Monad
import me.scf37.fpscala2.dao.TodoDao
import me.scf37.fpscala2.module.DaoModule
import me.scf37.fpscala2.module.init.Init
import me.scf37.fpscala2.util.AsyncState

object MemoryDaoModule:
  def apply[I[_], F[_]: Monad](using init: Init[I, F], st: AsyncState[F, MemoryTodoDaoState]): DaoModule[I, F] = DaoModule(
    todoDao = init.delay(new MemoryTodoDao[F])
  )
