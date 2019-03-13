package me.scf37.fpscala2.controller

import me.scf37.fpscala2.model.Todo

trait TodoController[F[_]] {

  def list(): F[Seq[Todo]]

  def create(todo: Todo): F[Todo]

  def update(todo: Todo): F[Todo]

  def delete(id: String): F[Boolean]
}
