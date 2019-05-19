package me.scf37.fpscala2

import cats.Monad
import cats.effect.Effect
import cats.effect.Sync
import cats.implicits._
import me.scf37.fpscala2.db.sql.SqlEffectEval
import me.scf37.fpscala2.db.sql.SqlEffectLift
import me.scf37.fpscala2.module.CommonModule
import me.scf37.fpscala2.module.ControllerModule
import me.scf37.fpscala2.module.DaoModule
import me.scf37.fpscala2.module.DbModule
import me.scf37.fpscala2.module.Later
import me.scf37.fpscala2.module.ServerModule
import me.scf37.fpscala2.module.ServiceModule
import me.scf37.fpscala2.module.WebModule
import me.scf37.fpscala2.module.config.ApplicationConfig

class Application[I[_]: Later: Monad, F[_]: Effect, DbEffect[_]: Sync](
  config: ApplicationConfig
)(
  implicit
  DB: SqlEffectLift[F, DbEffect],
  DE: SqlEffectEval[F, DbEffect]
) {


  val commonModule: I[CommonModule[I, F]] = Later[I].later(CommonModule[I, F](config.json))

  val dbModule: I[DbModule[I, F, DbEffect]] =
    Later[I].later(DbModule[I, F, DbEffect](config.db))

  val daoModule: I[DaoModule[I, DbEffect]] =
    Later[I].later(DaoModule[I, F, DbEffect])

  val serviceModule: I[ServiceModule[I, DbEffect]] = for {
    daoModule <- daoModule
  } yield ServiceModule[I, DbEffect](daoModule)


  val controllerModule: I[ControllerModule[I, F]] = for {
    commonModule <- commonModule
    serviceModule <- serviceModule
    dbModule <- dbModule
  } yield ControllerModule[I, F, DbEffect](
    commonModule, serviceModule, dbModule)

  val webModule: I[WebModule[I, F]] = for {
    controllerModule <- controllerModule
    commonModule <- commonModule
  } yield WebModule[I, F](controllerModule, commonModule)

  val serverModule: I[ServerModule[I, F]] = for {
    webModule <- webModule
    commonModule <- commonModule
  } yield ServerModule[I, F](
    webModule, commonModule, config.server)


}