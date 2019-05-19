package me.scf37.fpscala2.module


import cats.MonadError

import scala.util.Try

/**
  * Lazy monad.
  *
  * It is synchronous monad with built in memoization, capable to capture exceptions.
  *
  * Unlike cats.Eval.later, memoization is kept for map/flatMap operations as well which is
  * required for initialization effect to make singletons
  *
  * @param f
  * @tparam T
  */
class Lazy[T] private (f: => Either[Throwable, T]) {
  lazy val value: Either[Throwable, T] = mock.map(Right.apply).getOrElse(f)

  @volatile
  var mock: Option[T] = None
}

object Lazy {
  def apply[T](value: => T): Lazy[T] = new Lazy(Try(value).toEither)

  implicit val MonadError: MonadError[Lazy, Throwable] = new MonadError[Lazy, Throwable] {

    override def flatMap[A, B](fa: Lazy[A])(f: A => Lazy[B]): Lazy[B] = new Lazy(
      fa.value.flatMap(v => f(v).value)
    )

    override def tailRecM[A, B](a: A)(f: A => Lazy[Either[A, B]]): Lazy[B] = ???

    override def raiseError[A](e: Throwable): Lazy[A] = new Lazy(Left(e))

    override def handleErrorWith[A](fa: Lazy[A])(f: Throwable => Lazy[A]): Lazy[A] = ???

    override def pure[A](x: A): Lazy[A] = new Lazy[A](Right(x))
  }
}


