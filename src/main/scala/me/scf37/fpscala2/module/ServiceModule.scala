package me.scf37.fpscala2.module

import cats.{Monad, MonadThrow}
import cats.effect.Sync
import cats.implicits._
import me.scf37.fpscala2.service.TodoService
import me.scf37.fpscala2.service.impl.TodoServiceImpl

case class ServiceModule[I[_], F[_]](
  todoService: I[TodoService[F]]
)

object ServiceModule:
  def apply[I[_]: Monad, F[_]: MonadThrow](daoModule: DaoModule[I, F]): ServiceModule[I, F] =
    ServiceModule[I, F](
      todoService =
        for
          dao <- daoModule.todoDao
        yield new TodoServiceImpl[F](dao)
    )

