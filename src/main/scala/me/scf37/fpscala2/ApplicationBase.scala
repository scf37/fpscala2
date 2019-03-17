package me.scf37.fpscala2

import cats.Monad
import cats.effect.Effect
import cats.effect.Sync
import me.scf37.fpscala2.module.CommonModule
import me.scf37.fpscala2.module.CommonModuleImpl
import me.scf37.fpscala2.module.ControllerModule
import me.scf37.fpscala2.module.ControllerModuleImpl
import me.scf37.fpscala2.module.DaoModule
import me.scf37.fpscala2.module.DbModule
import me.scf37.fpscala2.module.Later
import me.scf37.fpscala2.module.ServerModule
import me.scf37.fpscala2.module.ServerModuleImpl
import me.scf37.fpscala2.module.ServiceModule
import me.scf37.fpscala2.module.ServiceModuleImpl
import me.scf37.fpscala2.module.WebModule
import me.scf37.fpscala2.module.WebModuleImpl
import me.scf37.fpscala2.module.config.ApplicationConfig

/**
  * Assembled application that is abstracted away from database layer implementation
  *
  * @param config
  * @tparam I application classes initialization effect
  * @tparam F generic effect
  * @tparam DbEffect database effect
  */
abstract class ApplicationBase[I[_]: Later: Monad, F[_]: Effect, DbEffect[_]: Sync](
  config: ApplicationConfig
) {

  lazy val commonModule: CommonModule[F, I] = new CommonModuleImpl[F, I](config.json)

  def dbModule: DbModule[F, DbEffect, I]

  def daoModule: DaoModule[DbEffect, I]

  lazy val serviceModule: ServiceModule[DbEffect, I] = new ServiceModuleImpl[DbEffect, I](daoModule)

  lazy val controllerModule: ControllerModule[F, I] = new ControllerModuleImpl[F, DbEffect, I](
    commonModule, serviceModule, dbModule)

  lazy val webModule: WebModule[F, I] = new WebModuleImpl[F, I](controllerModule, commonModule)

  lazy val serverModule: ServerModule[F, I] = new ServerModuleImpl[F, I](
    webModule, commonModule, config.server)
}
