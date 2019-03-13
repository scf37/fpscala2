package me.scf37.fpscala2.service

import me.scf37.fpscala2.model.Todo

trait TodoService[F[_]] {
  def list(): F[Seq[Todo]]

  def create(todo: Todo): F[Todo]

  def update(todo: Todo): F[Todo]

  def delete(id: String): F[Boolean]
}
