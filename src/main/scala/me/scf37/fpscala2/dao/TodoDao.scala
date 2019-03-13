package me.scf37.fpscala2.dao

import me.scf37.fpscala2.model.Todo

trait TodoDao[F[_]] {
  def list(): F[Seq[Todo]]

  def get(id: String): F[Option[Todo]]

  def save(todo: Todo): F[Todo]

  def delete(id: String): F[Boolean]
}
