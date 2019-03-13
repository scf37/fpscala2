package me.scf37.fpscala2.controller.impl

import me.scf37.fpscala2.controller.TodoController
import me.scf37.fpscala2.db.TxManager
import me.scf37.fpscala2.model.Todo
import me.scf37.fpscala2.service.TodoService

class TodoControllerImpl[F[_], T[_]](
  service: TodoService[T],
  tx: TxManager[F, T]
) extends TodoController[F] {

  override def list(): F[Seq[Todo]] = {
    tx.tx(service.list())
  }

  override def create(todo: Todo): F[Todo] = {
    tx.tx(service.create(todo))
  }

  override def update(todo: Todo): F[Todo] = {
    tx.tx(service.update(todo))
  }

  override def delete(id: String): F[Boolean] = {
    tx.tx(service.delete(id))
  }
}
