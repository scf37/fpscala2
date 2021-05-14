package me.scf37.fpscala2.module

import cats.{Monad, MonadThrow}
import cats.effect.Sync
import cats.implicits._
import me.scf37.fpscala2.module.init.Init
import me.scf37.fpscala2.service.TodoService
import me.scf37.fpscala2.service.impl.TodoServiceImpl

case class ServiceModule[I[_], F[_]](
  todoService: I[TodoService[F]]
):
  def mapK[II[_]](f: [A] => I[A] => II[A]): ServiceModule[II, F] = ServiceModule(
    todoService = f(todoService)
  )


object ServiceModule:
  def apply[I[_]: Monad, F[_], DbEffect[_]: MonadThrow](daoModule: DaoModule[I, DbEffect])(
    using init: Init[I, F]
  ): ServiceModule[I, DbEffect] =
    ServiceModule[I, DbEffect](
      todoService = daoModule.todoDao.map { dao =>
        new TodoServiceImpl[DbEffect](dao)
      }
    )

