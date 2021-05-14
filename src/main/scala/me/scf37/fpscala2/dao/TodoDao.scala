package me.scf37.fpscala2.dao

import me.scf37.fpscala2.model.Todo

/**
  * Dao for todo items
  *
  * @tparam F
  */
trait TodoDao[F[_]]:
  def list(): F[Seq[Todo]]

  def get(id: String): F[Option[Todo]]

  /**
    * Save Todo item. If todo.id is defined in the database, it is update. Otherwise, it is insert.
    * Kind of unusual and selected to showcase more complex logic in service layer
    *
    * @param todo
    * @return
    */
  def save(todo: Todo): F[Todo]

  def delete(id: String): F[Boolean]
