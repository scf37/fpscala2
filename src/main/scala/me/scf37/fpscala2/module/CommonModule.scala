package me.scf37.fpscala2.module

import cats.Monad
import cats.effect.Sync
import me.scf37.fpscala2.logging.Log
import me.scf37.fpscala2.logging.LogImpl
import me.scf37.fpscala2.service.JsonService

case class CommonModule[I[_], F[_]](
  json: I[JsonService[F]],
  log: I[Log[F]]
)

object CommonModule:

  def apply[I[_]: Later: Monad, F[_]: Sync]: CommonModule[I, F] = CommonModule[I, F](
    json = Later[I].later {
      JsonService[F]
    },

    log = Later[I].later {
      new LogImpl[F]
    }
  )

