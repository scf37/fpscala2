package me.scf37.fpscala2.typeclass
import cats.Applicative
import cats.MonadError
import cats.implicits.given

/**
  * SyncResource is synchronous version of cats.effect.Resource tailored for single-threaded use.
  * It does not support fiber cancellation but in return can work on any MonadError including Either[*, Throwable]
  *
  * @param f
  * @tparam F
  * @tparam A
  */
case class SyncResource[F[_], A](private val f: () => F[(A, () => F[Unit])]):
  /**
    * Evalutate this resource, call [f] on resource value and then close the resource
    */
  def use[B](f: A => F[B])(using M: MonadError[F, Throwable]): F[B] = this.f().flatMap { case (a, closeFun) =>
    try
      f(a).flatMap { b =>
        closeFun().attempt.map(_ => b)
      }
    catch
      case e => // f(a) failed
        try
          closeFun().attempt.flatMap(_ => e.raiseError)
        catch
          case _ => e.raiseError // closeFun() failed
  }


object SyncResource:
  given monadErrorInstance[F[_], E](using MonadError[F, E]): MonadError[SyncResource[F, *], E] = new MonadError[SyncResource[F, *], E] {
    override def pure[A](f: A): SyncResource[F, A] = SyncResource(() => ((f, () => ().pure).pure))

    override def flatMap[A, B](f: SyncResource[F, A])(ff: A => SyncResource[F, B]): SyncResource[F, B] = SyncResource { () =>
      def ensure(f: () => F[Unit]): F[Unit] =
        try
          f()
        catch
          case _ => (()).pure

      for
        a_close <- f.f()
        (a, close) = a_close
        aa_close2 <- ff(a).f()
        (aa, close2) = aa_close2
      yield aa -> (() => ensure(close).flatMap(_ => ensure(close2)))
    }

    override def tailRecM[A, B](a: A)(f: A => SyncResource[F, Either[A, B]]): SyncResource[F, B] = ???

    override def handleErrorWith[A](fa: SyncResource[F, A])(f: E => SyncResource[F, A]): SyncResource[F, A] = SyncResource { () =>
      fa.f().handleErrorWith(e => f(e).f())
    }

    override def raiseError[A](e: E): SyncResource[F, A] = SyncResource(() => e.raiseError)
  }

  def fromAutoCloseable[F[_]: Applicative, A <: AutoCloseable, E](a: => A): SyncResource[F, A] = SyncResource(() => (a -> (() => a.close().pure)).pure)

