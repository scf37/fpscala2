package me.scf37.fpscala2.module

import cats.Monad
import cats.effect.Sync
import me.scf37.fpscala2.logging.Log
import me.scf37.fpscala2.logging.LogImpl
import me.scf37.fpscala2.module.config.JsonConfig
import me.scf37.fpscala2.service.JsonService

trait CommonModule[I[_], F[_]] {
  def json: I[JsonService[F]]

  def log: I[Log[F]]
}

object CommonModule {

  def apply[I[_]: Later: Monad, F[_]: Sync](
    jsonConfig: JsonConfig
  ) = new CommonModule[I, F] {

    override val json: I[JsonService[F]] = Later[I].later {
      JsonService[F]
    }

    override val log: I[Log[F]] = Later[I].later {
      new LogImpl[F]
    }
  }
}
