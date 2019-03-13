package me.scf37.fpscala2.db.sql

import java.sql.Connection

import cats.effect.ExitCase
import cats.effect.Sync
import me.scf37.fpscala2.db.Db
import cats.implicits._
import scala.util.Try

/**
  * Monad for JDBC database effect.
  * Wrapped computation takes jdbc connection and returns value or exception
  *
  */
case class SqlDb[T](f: Connection => Either[Throwable, T])

object SqlDb {
  implicit val db = new Db[SqlDb] {
    override def eval[T](f: Connection => T): SqlDb[T] = SqlDb(lift(f))
  }

  implicit val sync: Sync[SqlDb] = new Sync[SqlDb] {

    override def suspend[A](thunk: => SqlDb[A]): SqlDb[A] = SqlDb(liftEither(c => thunk.f(c)))

    override def bracketCase[A, B](acquire: SqlDb[A])(use: A => SqlDb[B])(release: (A, ExitCase[Throwable]) => SqlDb[Unit]): SqlDb[B] =
      for {
        a <- acquire
        b <- use(a).attempt.flatMap {
          // when release returns an error, we silently ignore it
          case Left(e) => release(a, ExitCase.error(e)).handleError(_ => Unit).flatMap(_ => raiseError[B](e))

          case Right(b) => release(a, ExitCase.complete).map(_ => b)
        }
      } yield b

    override def flatMap[A, B](fa: SqlDb[A])(f: A => SqlDb[B]): SqlDb[B] =
      SqlDb(c => fa.f(c).flatMap(a => f(a).f(c)))

    override def tailRecM[A, B](a: A)(f: A => SqlDb[Either[A, B]]): SqlDb[B] = SqlDb(c => {
      var aNext = a
      var loop = true
      var res: Either[Throwable, B] = null

      try {
        while (loop) {
          f(aNext).f(c) match {

            case Left(ex) =>
              res = Left(ex)
              loop = false

            case Right(Left(aa)) =>
              aNext = aa

            case Right(Right(b)) =>
              res = Right(b)
              loop = false
          }
        }
        res
      } catch {
        case e: Throwable => Left(e)
      }

    })

    override def raiseError[A](e: Throwable): SqlDb[A] = SqlDb(_ => Left(e))

    override def handleErrorWith[A](fa: SqlDb[A])(f: Throwable => SqlDb[A]): SqlDb[A] =
      SqlDb(c => fa.f(c).left.flatMap(ex => f(ex).f(c)))

    override def pure[A](x: A): SqlDb[A] = SqlDb(_ => Right(x))
  }

  private def lift[A](f: Connection => A): Connection => Either[Throwable, A] = c => Try(f(c)).toEither
  private def liftEither[A](f: Connection => Either[Throwable, A]): Connection => Either[Throwable, A] =
    c => Try(f(c)).toEither.flatMap(identity)
}
