package me.scf37.fpscala2.db.sql

import java.sql.Connection
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import cats.arrow.FunctionK
import cats.effect.Async
import cats.~>
import javax.sql.DataSource
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
class SqlTxManager[F[_]: Async](ds: DataSource, poolSize: Int) extends TxManager[F, SqlDb] {

  private val jdbcPool = Executors.newFixedThreadPool(poolSize, new ThreadFactory {
    private val id = new AtomicInteger()
    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r)
      t.setDaemon(true)
      t.setName("jdbc-pool-" + id.incrementAndGet())
      t
    }
  })

  override def tx: SqlDb ~> F = FunctionK.lift(doTx)

  private def doTx[A](t: SqlDb[A]): F[A] = Async[F].async { cb =>
    jdbcPool.submit(() => {
      cb(inTransaction(t.f))
    }.asInstanceOf[Runnable])
  }

  private def inTransaction[T](f: Connection => Either[Throwable, T]): Either[Throwable, T] = Try {
    withConnection { conn =>
      conn.setAutoCommit(false)
      try {
        val r = f(conn)
        conn.commit()
        r
      } catch {
        case e: Throwable =>
          conn.rollback()
          Left(e)
      }
    }
  }.toEither.flatMap(identity)

  private def withConnection[T](f: Connection => T): T = {
    val conn = ds.getConnection
    try {
      f(conn)
    } finally {
      Try(conn.close())
    }
  }
}
