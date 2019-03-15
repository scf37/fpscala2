package me.scf37.fpscala2.controller

import me.scf37.fpscala2.model.Todo

/**
  * Controller level - API ready to be exposed to external clients
  *
  * @tparam F
  */
trait TodoController[F[_]] {

  def list(): F[Seq[Todo]]

  def get(id: String): F[Todo]

  def create(id: String, todo: TodoRequest): F[Todo]

  def update(id: String, todo: TodoRequest): F[Todo]

  def delete(id: String): F[Boolean]
}
