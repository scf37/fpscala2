package me.scf37.fpscala2.logging

import cats.effect.Sync
import org.slf4j.LoggerFactory
import cats.implicits._

class LogImpl[F[_]: Sync] extends Log[F] {
  private val appLog = LoggerFactory.getLogger("app")
  private val auditLog = LoggerFactory.getLogger("audit")
  private val requestLog = LoggerFactory.getLogger("request")

  override def logInfo(msg: => String): F[Unit] = Sync[F].delay {
    appLog.info(msg)
  }

  def logAudit[T](operation: String, params: String*)(f: F[T]): F[T] = {
    def doLogAudit(isError: Boolean): F[Unit] = {
      val err = if (isError) "err" else "ok"
      val paramsStr = params.mkString("|")
      Sync[F].delay(auditLog.info(s"$operation|$err|$paramsStr"))
    }

    f.attempt.flatMap {
      case Left(ex) => doLogAudit(true).flatMap(_ => Sync[F].raiseError(ex))
      case Right(v) => doLogAudit(false).map(_ => v)
    }
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
