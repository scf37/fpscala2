package me.scf37.fpscala2.typeclass

import cats.data.Kleisli

/** evaluate FF to F with given context C */
trait Run[FF[_], F[_], C] {
  def run[A](f: FF[A])(c: C): F[A]
}

object Run:
  given kleisliInstnace[F[_], C]: Run[Kleisli[F, C, *], F, C] with
    override def run[A](f: Kleisli[F, C, A])(c: C): F[A] = f.run(c)