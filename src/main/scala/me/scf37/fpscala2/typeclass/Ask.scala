package me.scf37.fpscala2.typeclass

import cats.implicits._
import cats.Monad
import cats.Applicative
import cats.data.Kleisli

/**
  * Extract context of type C from F
  */
trait Ask[F[_], C]:
  self =>
  def ask: F[C]

object Ask:
  def apply[F[_], C](using A: Ask[F, C]): Ask[F, C] = summon

  given kleisliInstance[F[_]: Applicative, C]: Ask[Kleisli[F, C, *], C] with
    override def ask: Kleisli[F, C, C] = Kleisli.ask[F, C]

end Ask