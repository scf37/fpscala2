package me.scf37.fpscala2

import cats.Monad
import cats.effect.Effect
import cats.effect.Sync
import me.scf37.fpscala2.db.sql.SqlEffectEval
import me.scf37.fpscala2.db.sql.SqlEffectLift
import me.scf37.fpscala2.module.DaoModule
import me.scf37.fpscala2.module.DaoModuleImpl
import me.scf37.fpscala2.module.DbModule
import me.scf37.fpscala2.module.DbModuleImpl
import me.scf37.fpscala2.module.Later
import me.scf37.fpscala2.module.config.ApplicationConfig

class Application[I[_]: Later: Monad, F[_]: Effect, DbEffect[_]: Sync](
  config: ApplicationConfig
)(
  implicit
  DB: SqlEffectLift[F, DbEffect],
  DE: SqlEffectEval[F, DbEffect]
) extends ApplicationBase[I, F, DbEffect](config) {

  override lazy val dbModule: DbModule[I, F, DbEffect] = new DbModuleImpl[I, F, DbEffect](config.db)

  override lazy val daoModule: DaoModule[I, DbEffect] = new DaoModuleImpl[I, F, DbEffect]()
}