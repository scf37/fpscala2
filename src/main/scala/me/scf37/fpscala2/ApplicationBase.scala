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

  lazy val commonModule: CommonModule[I, F] = new CommonModuleImpl[I, F](config.json)

  def dbModule: DbModule[I, F, DbEffect]

  def daoModule: DaoModule[I, DbEffect]

  lazy val serviceModule: ServiceModule[I, DbEffect] = new ServiceModuleImpl[I, DbEffect](daoModule)

  lazy val controllerModule: ControllerModule[I, F] = new ControllerModuleImpl[I, F, DbEffect](
    commonModule, serviceModule, dbModule)

  lazy val webModule: WebModule[I, F] = new WebModuleImpl[I, F](controllerModule, commonModule)

  lazy val serverModule: ServerModule[I, F] = new ServerModuleImpl[I, F](
    webModule, commonModule, config.server)
}
