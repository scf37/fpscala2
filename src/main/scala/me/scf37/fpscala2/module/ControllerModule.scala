package me.scf37.fpscala2.module

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import me.scf37.fpscala2.controller.TodoController
import me.scf37.fpscala2.controller.impl.TodoControllerImpl

case class ControllerModule[I[_], F[_]](
  todoController: I[TodoController[F]]
):
  def mapK[II[_]](f: [A] => I[A] => II[A]): ControllerModule[II, F] = ControllerModule(
    todoController = f(todoController)
  )


object ControllerModule:
  def apply[I[_]: Monad, F[_]: Sync, DbEffect[_]](
    commonModule: CommonModule[I, F],
    serviceModule: ServiceModule[I, DbEffect],
    dbModule: DbModule[I, F, DbEffect]
  ): ControllerModule[I, F] = ControllerModule[I, F](
    todoController = (serviceModule.todoService, dbModule.tx, commonModule.log).mapN {
      case (todoService, tx, log) => new TodoControllerImpl[F, DbEffect](todoService, tx = tx, log = log)
    }
  )

