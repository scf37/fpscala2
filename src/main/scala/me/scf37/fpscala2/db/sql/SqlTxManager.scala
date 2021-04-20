package me.scf37.fpscala2.db.sql

import java.sql.Connection
import cats.arrow.FunctionK
import cats.effect.Async
import cats.effect.Sync
import cats.implicits._
import cats.~>

import javax.sql.DataSource
import me.scf37.fpscala2.db.TxManager

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * Transaction manager for JDBC
  *
  * JDBC transactions are run on separate thread pool to avoid suspensions of IO threads
  *
  * @param ds data source to use
  * @param jdbcPool thread pool for synchronous JDBC code
  * @param alwaysRollback Always rollback transaction, useful for tests
  * @tparam F generic effect
  */
class SqlTxManager[F[_]: Async, DbEffect[_]](
    ds: DataSource,
    jdbcPool: ExecutionContext,
    alwaysRollback: Boolean = false
  )(
    implicit DE: SqlEffectEval[F, DbEffect]
  ) extends TxManager[F, DbEffect]:

  override def tx: [A] => DbEffect[A] => F[A] = [A] => (dbEffect: DbEffect[A]) => doTx(dbEffect)

  private def doTx[A](t: DbEffect[A]): F[A] = Async[F].evalOn({
    inTransaction(c => DE.eval(t, c))
  }, jdbcPool)

  private def inTransaction[T](f: Connection => F[T]): F[T] =
    withConnection { conn =>
      conn.setAutoCommit(false)

      f(conn).attempt.flatMap {

        case Left(e) =>
          Sync[F].delay(conn.rollback()).flatMap(_ => Sync[F].raiseError(e))

        case Right(r) =>
          Sync[F].delay {
            if alwaysRollback then
              conn.rollback()
            else
              conn.commit()
            r
          }
      }
    }

  private def withConnection[T](f: Connection => F[T]): F[T] =
    val c: F[Connection] = Sync[F].delay(ds.getConnection)

    Sync[F].bracket(c)(f)(conn => Sync[F].delay(Try(conn.close())))

