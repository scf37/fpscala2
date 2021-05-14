package me.scf37.fpscala2.logging

import cats.Defer
import cats.MonadError
import cats.implicits._
import org.slf4j.LoggerFactory

class LogImpl[F[_]: Defer](
  implicit ME: MonadError[F, Throwable]
) extends Log[F]:
  private val appLog = LoggerFactory.getLogger("app")
  private val auditLog = LoggerFactory.getLogger("audit")
  private val requestLog = LoggerFactory.getLogger("request")
  
  private def delay[A](a: => A): F[A] = Defer[F].defer(a.pure[F])

  override def logInfo(msg: => String): F[Unit] = delay {
    appLog.info(msg)
  }

  def logAudit[T](operation: String, params: String*)(f: F[T]): F[T] =
    def doLogAudit(isError: Boolean): F[Unit] =
      val err = if (isError) "err" else "ok"
      val paramsStr = params.mkString("|")
      delay(auditLog.info(s"$operation|$err|$paramsStr"))

    f.attempt.flatMap {
      case Left(ex) => doLogAudit(true).flatMap(_ => ME.raiseError(ex))
      case Right(v) => doLogAudit(false).map(_ => v)
    }


  override def logRequest(msg: =>String): F[Unit] = delay {
    requestLog.info(msg)
  }

  override def logValidationError(msg: =>String, ex: Throwable): F[Unit] = delay {
    appLog.warn(msg, ex)
  }

  override def logUnexpectedError(msg: =>String, ex: Throwable): F[Unit] = delay {
    appLog.error(msg, ex)
  }
