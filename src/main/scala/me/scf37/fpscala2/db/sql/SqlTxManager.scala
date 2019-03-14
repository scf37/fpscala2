package me.scf37.fpscala2.db.sql

import java.sql.Connection

import cats.arrow.FunctionK
import cats.effect.ContextShift
import cats.effect.Sync
import cats.implicits._
import cats.~>
import javax.sql.DataSource
import me.scf37.fpscala2.db.DbEval
import me.scf37.fpscala2.db.TxManager

import scala.util.Try

/**
  * Transaction manager for JDBC
  *
  * JDBC transactions are run on separate thread pool to avoid suspensions of IO threads
  *
  * @param ds data source to use
  * @param poolSize maximum number of parallel transactions
  * @tparam F generic effect
  */
class SqlTxManager[F[_]: Sync, DbEffect[_]](
  ds: DataSource, jdbcPool: ContextShift[F]
)(
  implicit DE: DbEval[DbEffect, F]
) extends TxManager[F, DbEffect] {

  override def tx: DbEffect ~> F = FunctionK.lift(doTx)

  private def doTx[A](t: DbEffect[A]): F[A] = for {
    _ <- jdbcPool.shift
    r <- inTransaction(c => DE.eval(t, c))
  } yield r

  private def inTransaction[T](f: Connection => F[T]): F[T] =
    withConnection { conn =>
      conn.setAutoCommit(false)

      f(conn).attempt.flatMap {

        case Left(e) =>
          Sync[F].delay(conn.rollback()).flatMap(_ => Sync[F].raiseError(e))

        case Right(r) =>
          Sync[F].delay(conn.commit()).map(_ => r)
      }
    }

  private def withConnection[T](f: Connection => F[T]): F[T] = {
    val c: F[Connection] = Sync[F].delay(ds.getConnection)

    Sync[F].bracket(c)(f)(conn => Sync[F].delay(Try(conn.close())))
  }
}
