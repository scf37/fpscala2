package me.scf37.fpscala2.module

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import me.scf37.fpscala2.service.TodoService
import me.scf37.fpscala2.service.impl.TodoServiceImpl

trait ServiceModule[I[_], F[_]] {
  def todoService: I[TodoService[F]]
}
object ServiceModule {

  def apply[I[_]: Monad, F[_]: Sync](daoModule: DaoModule[I, F]): ServiceModule[I, F] =
    new ServiceModule[I, F] {

      override val todoService: I[TodoService[F]] = {
        for {
          dao <- daoModule.todoDao
        } yield new TodoServiceImpl[F](dao)
      }
    }
}
