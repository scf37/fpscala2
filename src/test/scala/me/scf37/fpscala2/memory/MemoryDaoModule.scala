package me.scf37.fpscala2.memory

import cats.Monad
import me.scf37.fpscala2.config.Later
import me.scf37.fpscala2.config.module.DaoModule
import me.scf37.fpscala2.dao.TodoDao

class MemoryDaoModule[F[_]: Monad, I[_]: Later] extends DaoModule[F, I] {
  override lazy val todoDao: I[TodoDao[F]] = Later[I].later {
    new MemoryTodoDao[F]
  }
}
