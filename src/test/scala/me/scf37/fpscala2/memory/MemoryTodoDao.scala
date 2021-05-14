package me.scf37.fpscala2.memory

import cats.Monad
import cats.implicits._
import me.scf37.fpscala2.dao.TodoDao
import me.scf37.fpscala2.model.Todo
import me.scf37.fpscala2.util.AsyncState

import scala.collection.mutable

class MemoryTodoDao[F[_]: Monad](using st: AsyncState[F, MemoryTodoDaoState]) extends TodoDao[F]:

  override def list(): F[Seq[Todo]] = st.get.map(_.todos)

  override def get(id: String): F[Option[Todo]] = st.get.map(_.todos.find(_.id == id))

  override def save(todo: Todo): F[Todo] = st.modify { st =>
    st.todos.indexWhere(_.id == todo.id) match
      case -1 =>
        st.copy(todos = st.todos :+ todo) -> todo
      case i =>
        st.copy(todos = st.todos.updated(i, todo)) -> todo

  }

  override def delete(id: String): F[Boolean] = st.modify { st =>
    st.todos.indexWhere(_.id == id) match
      case -1 =>
        st -> false
      case i =>
        st.copy(todos = st.todos.patch(i, Nil, 1)) -> true

  }


case class MemoryTodoDaoState(
  todos: Vector[Todo]
)
object MemoryTodoDaoState:
  val Nil = MemoryTodoDaoState(Vector.empty)
