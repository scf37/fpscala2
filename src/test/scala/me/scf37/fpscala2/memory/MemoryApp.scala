package me.scf37.fpscala2.memory

import java.sql.Connection

import cats.Monad
import cats.effect.Effect
import me.scf37.fpscala2.Application
import me.scf37.fpscala2.db.sql.SqlEffectEval
import me.scf37.fpscala2.db.sql.SqlEffectLift
import me.scf37.fpscala2.module.Later
import me.scf37.fpscala2.module.config.ApplicationConfig


object MemoryApp {
  /**
    * Construct application using in-memory DAO instead of database
    */
  def apply[I[_]: Later: Monad, F[_]: Effect](): Application[I, F, F] = {

    // mock implementation of SQL effect (unused)
    implicit val sqlEff = new SqlEffectLift[F, F] with SqlEffectEval[F, F] {

      override def lift[A](f: Connection => F[A]): F[A] = f(null)

      override def eval[A](f: F[A], c: Connection): F[A] = f
    }

    val app = new Application[I, F, F](ApplicationConfig.testConfig)

    Later[I].setMock(app.daoModule, new MemoryDaoModule[I, F])
    Later[I].setMock(app.dbModule, new MemoryDbModule[I, F])

    app
  }
}