package me.scf37.fpscala2.module.init

import cats.effect.Resource
import cats.effect.Sync
import cats.Parallel
import cats.Monad
import cats.syntax.all._
import java.util.concurrent.atomic.AtomicInteger

/**
  * Indi is dependency injection monad with following properties:
  * - it is lazy: wrapped dependency is initialized on first use
  * - it is memoizing: evaulated resources are singletons and cached by wrapped monad instance identity
  * - it is stateless: evaulation stores cached values in separate IndiContext
  * - it is parallel: Indi applicative evaluates values in parallel, speeding up initialization
  *
  * @tparam F
  * @tparam A
  */
sealed abstract class Indi[F[_], A]:
  def evalOn(ctx: IndiContext[F])(using Parallel[F], Monad[F]): F[A] = Indi.evalOn(this, ctx)

  def map[B](f: A => B): Indi[F, B] = flatMap(a => Indi.pure(f(a)))

  def flatMap[B](f: A => Indi[F, B]): Indi[F, B] = Indi.Bind(this, f, Indi.Identity[B])

  def par[B](i: Indi[F, B]): Indi[F, (A, B)] = Indi.Par(this, i, Indi.Identity[(A, B)])

  private[init] def id: Indi.Identity[A]


object Indi:
  def apply[F[_], A](r: Resource[F, A]): Indi[F, A] = Suspend(_ => r, Identity[A])

  def pure[F[_], A](a: A): Indi[F, A] = Allocate(a, Identity[A])

  def delay[F[_]: Sync, A](a: => A): Indi[F, A] = apply(Resource.eval(Sync[F].delay(a)))

  def par[F[_], A, B](a: Indi[F, A], b: Indi[F, B]): Indi[F, (A, B)] = Par(a, b, Identity[(A, B)])

  def applyWithContext[F[_], A](r: IndiContext[F] => Resource[F, A]): Indi[F, A] = Suspend(r, Identity[A])

  given monadInstance[F[_]]: Monad[Indi[F, *]] with
    override def pure[A](x: A): Indi[F, A] = Allocate(x, Identity[A])

    override def flatMap[A, B](fa: Indi[F, A])(f: A => Indi[F, B]): Indi[F, B] = Bind(fa, f, Identity[B])

    override def product[A, B](fa: Indi[F, A], fb: Indi[F, B]): Indi[F, (A, B)] = Par(fa, fb, Identity[(A, B)])

    override def ap[A, B](ff: Indi[F, A => B])(fa: Indi[F, A]): Indi[F, B] = map(product(fa, ff))(fa => fa._2(fa._1))

    override def tailRecM[A, B](a: A)(f: A => Indi[F, Either[A, B]]): Indi[F, B] = ???

  given initInstance[F[_]: Sync]: Init[Indi[F, *], F] with
    def delay[A](f: => A): Indi[F, A] = liftF(Sync[F].delay(f))
    def liftF[A](f: F[A]): Indi[F, A] = liftR(Resource.eval(f))
    def liftR[A](f: Resource[F, A]): Indi[F, A] = Indi(f)

  trait Identity[+A]
  object Identity {
    private val i = new AtomicInteger()
    def apply[A]: Identity[A] = new Identity[A] {
      val id = i.incrementAndGet().toString
      override def toString: String = id
    }
  }

  // lift value to memo
  case class Allocate[F[_], A](a: A, id: Identity[A]) extends Indi[F, A]:
    override def toString: String = getClass.getSimpleName + "#" + id

  // lift effect to memo, will be cached
  case class Suspend[F[_], A](fa: IndiContext[F] => Resource[F, A], id: Identity[A]) extends Indi[F, A]:
    override def toString: String = getClass.getSimpleName + "#" + id

  // parallel evaluation
  case class Par[F[_], A, B](a: Indi[F, A], b: Indi[F, B], id: Identity[(A, B)]) extends Indi[F, (A, B)]:
    override def toString: String = getClass.getSimpleName + "#" + id

  // sequential evaluation
  case class Bind[F[_], A, B](a: Indi[F, A], f: A => Indi[F, B], id: Identity[B]) extends Indi[F, B]:
    override def toString: String = getClass.getSimpleName + "#" + id

  private def evalOn[F[_]: Monad: Parallel, A](i: Indi[F, A], ctx: IndiContext[F]): F[A] =
    i match
      case Indi.Allocate(a, _) =>
        ctx.get(key(i), Resource.pure[F, A](a)).flatten

      case Indi.Suspend(fa, _) =>
        ctx.get[A](key[F, A](i), fa(ctx)).flatten

      case v: Indi.Par[F, _, _] =>
        ctx.get[A](key(i), Resource.eval {
          val deps: Vector[Indi[F, Any]] = expandPars(v) // allow par composition to be closed in parallel
          val xx: Vector[F[Any]] = deps.map(i => evalOn(i, ctx))
          xx.parSequence.flatMap { _ =>
            (evalOn(v.a, ctx), evalOn(v.b, ctx)).parMapN { (a, b) =>
              (a -> b).asInstanceOf[A]
            }
          }
        }).flatten

      case Indi.Bind(a, f, _) =>
        val load = evalOn(a, ctx).flatMap { aa =>
          evalOn(f(aa), ctx)
        }
        ctx.get(key(i), Resource.eval(load)).flatten


  private def expandPars[F[_]](i: Indi[F, _]): Vector[Indi[F, Any]] =
    i match
      case Par(a, b, id) => expandPars(a) ++ expandPars(b)
      case i => Vector(i.asInstanceOf[Indi[F, Any]]) // Scala3 does not like wildcards anymore so we have to cheat

  private def key[F[_], A](i: Indi[F, A]): Identity[A] = i.id


