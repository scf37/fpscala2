package me.scf37.fpscala2.typeclass
import cats.{Applicative, MonadError, MonadThrow}
import cats.implicits._

/**
  * SyncResource is synchronous version of cats.effect.Resource tailored for single-threaded use.
  * It does not support fiber cancellation but in return can work on any MonadError including Either[*, Throwable]
  *
  * @param f
  * @tparam F
  * @tparam A
  */
case class SyncResource[F[_], A](private val eval: () => F[(A, () => F[Unit])]):
  import SyncResource._
  /**
    * Evalutate this resource, call [f] on resource value and then close the resource
    */
  def use[B](f: A => F[B])(using M: MonadError[F, Throwable]): F[B] =
    this.eval().flatMap { case (a, closeFun) =>
      liftError(f(a))
        .onError { case _ => ensureF(closeFun()) }
        .flatMap { b =>
          ensureF(closeFun()).map(_ => b)
        }
    }


object SyncResource:
  given monadErrorInstance[F[_]](using MonadError[F, Throwable]): MonadError[SyncResource[F, *], Throwable] with
    override def pure[A](f: A): SyncResource[F, A] = SyncResource(() => ((f, () => ().pure).pure))

    override def flatMap[A, B](f: SyncResource[F, A])(ff: A => SyncResource[F, B]): SyncResource[F, B] = SyncResource { () =>

      f.eval().flatMap { case (a, close) =>
        liftError(ff(a).eval())
          .onError { case _ => ensureF(close()) }
          .map { case (aa, close2) =>
            aa -> (() => ensureF(close2()).flatMap(_ => ensureF(close())))
          }
      }
    }

    override def tailRecM[A, B](a: A)(f: A => SyncResource[F, Either[A, B]]): SyncResource[F, B] = ???

    override def handleErrorWith[A](fa: SyncResource[F, A])(f: Throwable => SyncResource[F, A]): SyncResource[F, A] = SyncResource { () =>
      fa.eval().handleErrorWith(e => f(e).eval())
    }

    override def raiseError[A](e: Throwable): SyncResource[F, A] = SyncResource(() => e.raiseError)


  def fromAutoCloseable[F[_]: Applicative, A <: AutoCloseable, E](a: => A): SyncResource[F, A] = SyncResource(() => {
    val evaluated = a
    (evaluated -> (() => evaluated.close().pure)).pure
  })

  private def liftError[F[_]: MonadThrow, A](f: => F[A]): F[A] =
    try
      f
    catch
      case e => e.raiseError

  private def ensureF[F[_]: MonadThrow, A](f: => F[A]): F[Unit] =
    try
      f.attempt.void
    catch
      case e => ().pure

