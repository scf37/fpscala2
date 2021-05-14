package me.scf37.fpscala2.module

import cats.Monad
import cats.effect.Sync
import me.scf37.fpscala2.logging.Log
import me.scf37.fpscala2.logging.LogImpl
import me.scf37.fpscala2.module.init.Init
import me.scf37.fpscala2.service.JsonService

case class CommonModule[I[_], F[_]](
  json: I[JsonService[F]],
  log: I[Log[F]]
):
  def mapK[II[_]](f: [A] => I[A] => II[A]): CommonModule[II, F] = CommonModule(
    json = f(json),
    log = f(log)
  )

object CommonModule:

  def apply[I[_]: Monad, F[_]: Sync](using init: Init[I, F]): CommonModule[I, F] = CommonModule[I, F](
    json = init delay JsonService[F],
    log = init delay new LogImpl[F]
  )

