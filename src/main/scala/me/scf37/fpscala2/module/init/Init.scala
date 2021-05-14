package me.scf37.fpscala2.module.init

import cats.effect.Resource
import cats.Applicative
import cats.Monad
import cats.implicits.given

/**
  * Initialization effect typeclass
  *
  * @tparam I initialization effect
  * @tparam F working effect
  */
trait Init[I[_], F[_]]:
  def delay[A](f: => A): I[A]
  def liftF[A](f: F[A]): I[A]
  def liftR[A](f: Resource[F, A]): I[A]


object Init:
  extension [F[_], A](v: F[A])
    def init[I[_]](using ap: Applicative[F], i: Init[I, F]): I[A] = i.liftF(v)
    def initF[I[_]](using ap: Applicative[F], i: Init[I, F]): I[A] = init

  extension [F[_], A](v: Resource[F, A])
    def init[I[_]](using ap: Applicative[F], i: Init[I, F]): I[A] = i.liftR(v)
    def initR[I[_]](using ap: Applicative[F], i: Init[I, F]): I[A] = init

  extension [I[_], F[_], A](v: I[Resource[F, A]])
    def init(using ap: Applicative[F], i: Init[I, F], M: Monad[I]): I[A] = v.flatMap(i.liftR)
    def initIR(using ap: Applicative[F], i: Init[I, F], M: Monad[I]): I[A] = init

  extension [I[_], F[_], A](v: I[F[A]])
    def initIF(implicit ap: Applicative[F], i: Init[I, F], M: Monad[I]): I[A] = v.flatMap(i.liftF)
