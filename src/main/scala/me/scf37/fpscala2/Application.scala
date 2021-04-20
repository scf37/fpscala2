package me.scf37.fpscala2

import cats.{Monad, MonadThrow}
import cats.effect.Sync
import cats.effect.Async
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
import cats.effect.std.Dispatcher

class Application[I[_]: Later: Monad, F[_]: Async : Dispatcher, DbEffect[_]: MonadThrow](
  config: ApplicationConfig
)(
  implicit
  DB: SqlEffectLift[DbEffect],
  DE: SqlEffectEval[F, DbEffect]
) {


  val commonModule: I[CommonModule[I, F]] = Later[I].later(CommonModule[I, F])

  val dbModule: I[DbModule[I, F, DbEffect]] =
    Later[I].later(DbModule[I, F, DbEffect](config.db, true))

  val daoModule: I[DaoModule[I, DbEffect]] =
    Later[I].later(DaoModule[I, DbEffect])

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