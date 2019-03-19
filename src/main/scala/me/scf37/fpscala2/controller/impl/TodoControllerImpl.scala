package me.scf37.fpscala2.controller.impl

import cats.MonadError
import cats.data.ValidatedNel
import cats.implicits._
import me.scf37.fpscala2.controller.TodoController
import me.scf37.fpscala2.controller.TodoRequest
import me.scf37.fpscala2.db.TxManager
import me.scf37.fpscala2.exception.ResourceNotFoundException
import me.scf37.fpscala2.exception.ValidationFailedException
import me.scf37.fpscala2.logging.Log
import me.scf37.fpscala2.model.Todo
import me.scf37.fpscala2.service.TodoService


class TodoControllerImpl[F[_], DbEffect[_]](
  service: TodoService[DbEffect],
  tx: TxManager[F, DbEffect],
  log: Log[F]
)(
  implicit ME: MonadError[F, Throwable]
) extends TodoController[F] {

  override def list(): F[Seq[Todo]] = log.logAudit("list") {
    tx.tx(service.list())
  }

  override def get(id: String): F[Todo] = log.logAudit("get", id)(for {
    todoOpt <- tx.tx(service.get(id))
    r <- todoOpt.fold(ME.raiseError[Todo](ResourceNotFoundException("Item not found")))(_.pure[F])
  } yield r)

  override def create(id: String, todo: ValidatedNel[String, TodoRequest]): F[Todo] = log.logAudit("create", id) {
    for {
      todo <- lift(todo)
      r <- tx.tx(service.create(parse(id, todo)))
    } yield  r
  }

  override def update(id: String, todo: ValidatedNel[String, TodoRequest]): F[Todo] = log.logAudit("update", id) {
    for {
      todo <- lift(todo)
      r <- tx.tx(service.update(parse(id, todo)))
    } yield  r
  }

  override def delete(id: String): F[Boolean] = log.logAudit("delete", id) {
    tx.tx(service.delete(id))
  }

  private def lift[A](validated: ValidatedNel[String, A]): F[A] =
    validated.fold[F[A]](
      errors => ME.raiseError(ValidationFailedException(errors.toList)),
      ME.pure
    )

  private def parse(id: String, todo: TodoRequest): Todo = Todo(id = id, text = todo.text)
}
