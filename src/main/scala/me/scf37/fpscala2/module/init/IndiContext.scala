package me.scf37.fpscala2.module.init
import cats.effect.Resource
import cats.effect.Async

/**
  * Stateful cache where Indi evaluation stores evaulated singletons
  *
  * @tparam F
  */
opaque type IndiContext[F[_]] = LoadingCache[F, Indi.Identity]

object IndiContext:
  def apply[F[_]: Async]: Resource[F, IndiContext[F]] = LoadingCache[F, Indi.Identity]

  extension [F[_]](c: IndiContext[F]) {
    private[init] def get[A](key: Indi.Identity[A], load: Resource[F, A]): F[F[A]] = c.get(key, load)
  }
