package me.scf37.fpscala2.logging

import cats.effect.Sync
import org.slf4j.LoggerFactory

class LogImpl[F[_]: Sync] extends Log[F] {
  private val appLog = LoggerFactory.getLogger("app")
  private val requestLog = LoggerFactory.getLogger("request")

  override def logInfo(msg: => String): F[Unit] = Sync[F].delay {
    appLog.info(msg)
  }

  override def logRequest(msg: =>String): F[Unit] = Sync[F].delay {
    requestLog.info(msg)
  }

  override def logValidationError(msg: =>String, ex: Throwable): F[Unit] = Sync[F].delay {
    appLog.warn(msg, ex)
  }

  override def logUnexpectedError(msg: =>String, ex: Throwable): F[Unit] = Sync[F].delay {
    appLog.error(msg, ex)
  }
}
