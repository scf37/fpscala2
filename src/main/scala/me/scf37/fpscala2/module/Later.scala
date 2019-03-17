package me.scf37.fpscala2.module

import cats.Eval

/**
  * Typeclass for monads supporting lazy evaluation and result memoization.
  * Both properties are required for initialization effect.
  *
  * @tparam I
  */
trait Later[I[_]] {
  def later[A](f: => A): I[A]
}

object Later {
  def apply[I[_]: Later]: Later[I] = implicitly[Later[I]]

  implicit val evalLater: Later[Eval] = new Later[Eval] {
    override def later[A](f: => A): Eval[A] = cats.Later(f)
  }
}
