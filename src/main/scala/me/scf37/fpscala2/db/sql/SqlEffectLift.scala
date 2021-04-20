package me.scf37.fpscala2.db.sql

import me.scf37.fpscala2.typeclass.Ask
import cats.Monad
import cats.implicits._
import java.sql.Connection


/**
  * Typeclass for lifting JDBC code into database effect.
  *
  * Database effect required java.sql.Connection instance for evaluation.
  */
trait SqlEffectLift[F[_]] {
  def lift[A](f: Connection => F[A]): F[A]
}

object SqlEffectLift {
  def apply[F[_]](implicit db: SqlEffectLift[F]): SqlEffectLift[F] = db

  given askInstance[F[_]: Monad] (using A: Ask[F, Connection]): SqlEffectLift[F] with
    def lift[A](f: Connection => F[A]): F[A] = A.ask.flatMap(f)
}

