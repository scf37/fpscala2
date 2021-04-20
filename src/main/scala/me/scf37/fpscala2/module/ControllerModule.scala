package me.scf37.fpscala2.module

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import me.scf37.fpscala2.controller.TodoController
import me.scf37.fpscala2.controller.impl.TodoControllerImpl

case class ControllerModule[I[_], F[_]](
  todoController: I[TodoController[F]]
)

object ControllerModule:
  def apply[I[_]: Monad, F[_]: Sync, DbEffect[_]](
    commonModule: CommonModule[I, F],
    serviceModule: ServiceModule[I, DbEffect],
    dbModule: DbModule[I, F, DbEffect]
  ): ControllerModule[I, F] = ControllerModule[I, F](
    todoController =
      for
        todoService <- serviceModule.todoService
        tx <- dbModule.tx
        log <- commonModule.log
      yield new TodoControllerImpl[F, DbEffect](todoService, tx = tx, log = log)
  )

