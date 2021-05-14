package me.scf37.fpscala2.module

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import cats.Monad
import cats.effect.Sync
import cats.effect.Async
import cats.implicits._

import javax.sql.DataSource
import me.scf37.fpscala2.db.TxManager
import me.scf37.fpscala2.db.sql.SqlEffectEval
import me.scf37.fpscala2.db.sql.SqlTxManager
import me.scf37.fpscala2.module.config.DbConfig
import me.scf37.fpscala2.module.init.Init
import org.apache.commons.dbcp2.DriverManagerConnectionFactory
import org.apache.commons.dbcp2.PoolableConnectionFactory
import org.apache.commons.dbcp2.PoolingDataSource
import org.apache.commons.pool2.impl.GenericObjectPool
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

case class DbModule[I[_], F[_], DbEffect[_]](
  tx: I[TxManager[F, DbEffect]]
):
  def mapK[II[_]](f: [A] => I[A] => II[A]): DbModule[II, F, DbEffect] = DbModule(
    tx = f(tx)
  )


object DbModule:

  def apply[I[_]: Monad, F[_]: Async, DbEffect[_]](
    config: I[DbConfig],
    alwaysRollback: Boolean = false
  )(using DE: SqlEffectEval[F, DbEffect],
          init: Init[I, F]
  ): DbModule[I, F, DbEffect] =

    val jdbcPool: I[ExecutionContext] =
      for
        config <- config
        jdbcPool =
          val pool = Executors.newFixedThreadPool(config.maxPoolSize, new ThreadFactory:
            private val id = new AtomicInteger()
            override def newThread(r: Runnable): Thread =
              val t = new Thread(r)
              t.setDaemon(true)
              t.setName("jdbc-pool-" + id.incrementAndGet())
              t
          )
          ExecutionContext.fromExecutor(pool)
      yield jdbcPool

    val flyway: I[Unit] =
      for
        config <- config
        _ =
          val flyway = Flyway.configure()
            .dataSource(config.url, config.user, config.password)
            .baselineOnMigrate(true)
            .load()

          // Start the migration
          flyway.migrate()
      yield ()

    val dataSource: I[DataSource] =
      for
        config <- config
        dataSource =
          val connectionFactory = new DriverManagerConnectionFactory(config.url, config.user, config.password)

          val poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null)

          val connectionPool = new GenericObjectPool(poolableConnectionFactory)

          connectionPool.setMinIdle(config.minPoolSize)
          connectionPool.setMaxTotal(config.maxPoolSize)

          poolableConnectionFactory.setPool(connectionPool)

          new PoolingDataSource(connectionPool)
      yield dataSource

    val tx: I[TxManager[F, DbEffect]] =
      for
        _ <- flyway
        tx <- (jdbcPool, dataSource).mapN { case (pool, dataSource) =>
          new SqlTxManager[F, DbEffect](dataSource, pool, alwaysRollback)
        }
      yield tx

    DbModule(tx = tx)
  end apply
