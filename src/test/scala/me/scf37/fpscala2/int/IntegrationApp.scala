package me.scf37.fpscala2.int

import cats.Monad
import cats.effect.Effect
import cats.effect.Sync
import me.scf37.fpscala2.Application
import me.scf37.fpscala2.db.sql.SqlEffectEval
import me.scf37.fpscala2.db.sql.SqlEffectLift
import me.scf37.fpscala2.module.DbModule
import me.scf37.fpscala2.module.Later
import me.scf37.fpscala2.module.config.ApplicationConfig
import me.scf37.fpscala2.module.config.DbConfig

object IntegrationApp {

  def apply[I[_] : Later : Monad, F[_] : Effect, DbEffect[_] : Sync](
    db: DbConfig,
    alwaysRollback: Boolean
  )(
    implicit
    DB: SqlEffectLift[F, DbEffect],
    DE: SqlEffectEval[F, DbEffect]
  ): Application[I, F, DbEffect] = {

    val cfg = ApplicationConfig.testConfig.copy(db = db)

    val app = new Application[I, F, DbEffect](cfg)

    Later[I].setMock(app.dbModule,
      DbModule[I, F, DbEffect](cfg.db, alwaysRollback = alwaysRollback)
    )

    app
  }
}