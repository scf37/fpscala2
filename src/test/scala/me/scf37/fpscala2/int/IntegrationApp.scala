package me.scf37.fpscala2.int

import cats.Monad
import cats.effect.Effect
import cats.effect.Sync
import me.scf37.fpscala2.Application
import me.scf37.fpscala2.db.Db
import me.scf37.fpscala2.db.DbEval
import me.scf37.fpscala2.module.DbModuleImpl
import me.scf37.fpscala2.module.Later
import me.scf37.fpscala2.module.config.ApplicationConfig
import me.scf37.fpscala2.module.config.DbConfig

class IntegrationApp[I[_] : Later : Monad, F[_] : Effect, DbEffect[_] : Sync](
  config: ApplicationConfig
)(
  implicit
  DB: Db[DbEffect, F],
  DE: DbEval[DbEffect, F]
) extends Application[I, F, DbEffect](config) {

  override lazy val dbModule = new DbModuleImpl[F, DbEffect, I](config.db, alwaysRollback = true)
}

object IntegrationApp {

  def make[I[_] : Later : Monad, F[_] : Effect, DbEffect[_] : Sync](
    db: DbConfig
  )(
    implicit
    DB: Db[DbEffect, F],
    DE: DbEval[DbEffect, F]
  ): IntegrationApp[I, F, DbEffect] = {

    val cfg = ApplicationConfig.testConfig.copy(db = db)

    new IntegrationApp[I, F, DbEffect](cfg)
  }
}