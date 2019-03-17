package me.scf37.fpscala2.memory

import cats.Monad
import cats.implicits._
import me.scf37.fpscala2.dao.TodoDao
import me.scf37.fpscala2.model.Todo

import scala.collection.mutable

class MemoryTodoDao[F[_]: Monad] extends TodoDao[F] {
  private val todos = mutable.Buffer.empty[Todo]

  override def list(): F[Seq[Todo]] = todos.toSeq.pure[F]

  override def get(id: String): F[Option[Todo]] = todos.find(_.id == id).pure[F]

  override def save(todo: Todo): F[Todo] = todos.indexWhere(_.id == todo.id) match {
    case -1 =>
      todos.append(todo)
      todo.pure[F]
    case i =>
      todos.update(i, todo)
      todo.pure[F]
  }

  override def delete(id: String): F[Boolean] = todos.indexWhere(_.id == id) match {
    case -1 =>
      false.pure[F]
    case i =>
      todos.remove(i)
      true.pure[F]
  }
}
