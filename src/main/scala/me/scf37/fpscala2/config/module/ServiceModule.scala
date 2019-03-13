package me.scf37.fpscala2.config.module

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import me.scf37.fpscala2.service.TodoService
import me.scf37.fpscala2.service.impl.TodoServiceImpl

trait ServiceModule[F[_], I[_]] {
  def todoService: I[TodoService[F]]
}

class ServiceModuleImpl[F[_]: Sync, I[_]: Monad](daoModule: DaoModule[F, I]) extends ServiceModule[F, I] {
  override lazy val todoService: I[TodoService[F]] = {
    for {
      dao <- daoModule.todoDao
    } yield new TodoServiceImpl[F](dao)
  }
}
