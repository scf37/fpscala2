package me.scf37.fpscala2

import cats.Monad
import cats.effect.IO
import me.scf37.fpscala2.config.ApplicationConfig
import me.scf37.fpscala2.config.Later
import me.scf37.fpscala2.config.module.CommonModule
import me.scf37.fpscala2.config.module.CommonModuleImpl
import me.scf37.fpscala2.config.module.ControllerModule
import me.scf37.fpscala2.config.module.ControllerModuleImpl
import me.scf37.fpscala2.config.module.DaoModule
import me.scf37.fpscala2.config.module.DaoModuleImpl
import me.scf37.fpscala2.config.module.DbModule
import me.scf37.fpscala2.config.module.DbModuleImpl
import me.scf37.fpscala2.config.module.ServerModule
import me.scf37.fpscala2.config.module.ServerModuleImpl
import me.scf37.fpscala2.config.module.ServiceModule
import me.scf37.fpscala2.config.module.ServiceModuleImpl
import me.scf37.fpscala2.config.module.WebModule
import me.scf37.fpscala2.config.module.WebModuleImpl
import me.scf37.fpscala2.db.sql.SqlDb

class Application[I[_]: Later: Monad](config: ApplicationConfig) {

  lazy val commonModule: CommonModule[IO, I] = new CommonModuleImpl[IO, I](config.json)

  lazy val dbModule: DbModule[IO, SqlDb, I] = new DbModuleImpl[IO, I](config.db)

  lazy val daoModule: DaoModule[SqlDb, I] = new DaoModuleImpl[SqlDb, I]

  lazy val serviceModule: ServiceModule[SqlDb, I] = new ServiceModuleImpl[SqlDb, I](daoModule)

  lazy val controllerModule: ControllerModule[IO, I] = new ControllerModuleImpl[IO, SqlDb, I](serviceModule, dbModule)

  lazy val webModule: WebModule[IO, I] = new WebModuleImpl[IO, I](controllerModule, commonModule)

  lazy val serverModule: ServerModule[IO, I] = new ServerModuleImpl[I](webModule, config.server)
}
