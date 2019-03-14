package me.scf37.fpscala2.config.module

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import cats.Monad
import cats.effect.Async
import cats.effect.ContextShift
import javax.sql.DataSource
import me.scf37.fpscala2.config.DbConfig
import me.scf37.fpscala2.config.Later
import me.scf37.fpscala2.db.TxManager
import me.scf37.fpscala2.db.sql.SqlDb
import me.scf37.fpscala2.db.sql.SqlTxManager
import org.apache.commons.dbcp2.DriverManagerConnectionFactory
import org.apache.commons.dbcp2.PoolableConnectionFactory
import org.apache.commons.dbcp2.PoolingDataSource
import org.apache.commons.pool2.impl.GenericObjectPool
import cats.implicits._
import me.scf37.fpscala2.db.DbEval
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

trait DbModule[F[_], DbEffect[_], I[_]] {
  def tx: I[TxManager[F, DbEffect]]
}

class DbModuleImpl[F[_]: Async, DbEffect[_], I[_]: Later: Monad](
  config: DbConfig
)(implicit DE: DbEval[DbEffect, F]
) extends DbModule[F, DbEffect, I] {

  override lazy val tx: I[TxManager[F, DbEffect]] = for {
    _ <- flyway
    pool <- jdbcPool
    dataSource <- dataSource
  } yield  {
    new SqlTxManager[F, DbEffect](dataSource, pool)
  }

  private lazy val jdbcPool: I[ContextShift[F]] = Later[I].later {
      val jdbcPool = Executors.newFixedThreadPool(config.maxPoolSize, new ThreadFactory {
        private val id = new AtomicInteger()
        override def newThread(r: Runnable): Thread = {
          val t = new Thread(r)
          t.setDaemon(true)
          t.setName("jdbc-pool-" + id.incrementAndGet())
          t
        }
      })

    new ContextShift[F] {
      override def shift: F[Unit] =
        Async[F].async(cb => {
          jdbcPool.submit(() => {
            cb(Right(()))
          }.asInstanceOf[Runnable])
        })

      override def evalOn[A](ec: ExecutionContext)(fa: F[A]): F[A] = ???
    }
  }

  private lazy val flyway: I[Unit] = Later[I].later {
    val flyway = Flyway.configure()
      .dataSource(config.url, config.user, config.password)
      .baselineOnMigrate(true)
      .load()

    // Start the migration
    flyway.migrate()
  }

  private lazy val dataSource: I[DataSource] = Later[I].later {
    val connectionFactory = new DriverManagerConnectionFactory(config.url, config.user, config.password)

    val poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null)

    val connectionPool = new GenericObjectPool(poolableConnectionFactory)

    connectionPool.setMinIdle(config.minPoolSize)
    connectionPool.setMaxTotal(config.maxPoolSize)

    poolableConnectionFactory.setPool(connectionPool)

    new PoolingDataSource(connectionPool)
  }
}