package me.scf37.fpscala2.config.module

import cats.Monad
import cats.implicits._
import me.scf37.fpscala2.controller.TodoController
import me.scf37.fpscala2.controller.impl.TodoControllerImpl

trait ControllerModule[F[_], I[_]] {
  def todoController: I[TodoController[F]]
}

class ControllerModuleImpl[F[_], T[_], I[_]: Monad](
  serviceModule: ServiceModule[T, I],
  dbModule: DbModule[F, T, I]
) extends ControllerModule[F, I] {

  override lazy val todoController: I[TodoController[F]] = for {
    todoService <- serviceModule.todoService
    tx <- dbModule.tx
  } yield new TodoControllerImpl[F, T](todoService, tx = tx)
}
