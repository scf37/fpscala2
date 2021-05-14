package me.scf37.fpscala2.util

import cats.Monad
import cats.effect.Ref
import cats.implicits._
import me.scf37.fpscala2.typeclass.Ask

/**
  * Typeclass for managing test state.
  *
  * Core idea is to keep test fake (in-memory implementation) pure, providing state via Kleisli monad, therefore
  * application under test is stateless and can be tested in parallel with easy examination of new state
  *
  * Unlike IndexedState monad, default implementation supports Parallel and Concurrent
  */
trait AsyncState[F[_], A]:
  def get: F[A]
  def update(f: A => A): F[Unit]
  def modify[B](f: A => (A, B)): F[B]
  /** Extract typeclass for substate B */
  def extract[B](get: A => B, set: (A, B) => A): AsyncState[F, B]


object AsyncState {
  /**
    * Make new AsyncState instance, given Kleisli-like effect G and underlying effect F
    */
  def make[G[_]: Monad, F[_]: Monad, A](lift: [A] => F[A] => G[A])(
    implicit A: Ask[G, Ref[F, A]]
  ): AsyncState[G, A] = new AsyncState[G, A]:
    self =>
    override def get: G[A] = A.ask.flatMap(ref => lift(ref.get))

    override def update(f: A => A): G[Unit] = modify(a => f(a) -> (()))

    override def modify[B](f: A => (A, B)): G[B] = A.ask.flatMap(ref => lift(ref.modify(f)))

    override def extract[B](get: A => B, set: (A, B) => A): AsyncState[G, B] = doExtract(this, get , set)

  private def doExtract[F[_]: Monad, A, C](s: AsyncState[F, A], get1: A => C, set: (A, C) => A): AsyncState[F, C] = new AsyncState[F, C]:
    override def get: F[C] = s.get.map(get1)

    override def update(f: C => C): F[Unit] = modify(a => f(a) -> (()))

    override def modify[B](f: C => (C, B)): F[B] = s.modify { a =>
      val (c, b) = f(get1(a))
      set(a, c) -> b
    }

    override def extract[B](get: C => B, set: (C, B) => C): AsyncState[F, B] = doExtract(this, get , set)

}
