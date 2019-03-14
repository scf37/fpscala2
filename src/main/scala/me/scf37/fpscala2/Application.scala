package me.scf37.fpscala2

import cats.Monad
import cats.effect.Async
import cats.effect.Effect
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


class ApplicationBackend[I[_]: Later: Monad, F[_]: Async](config: ApplicationConfig) {
  import me.scf37.fpscala2.db.sql.db

  type SqlDbF[A] = SqlDb[F, A]

  lazy val commonModule: CommonModule[F, I] = new CommonModuleImpl[F, I](config.json)

  lazy val dbModule: DbModule[F, SqlDbF, I] = new DbModuleImpl[F, I](config.db)

  lazy val daoModule: DaoModule[SqlDbF, I] = new DaoModuleImpl[SqlDbF, I]

  lazy val serviceModule: ServiceModule[SqlDbF, I] = new ServiceModuleImpl[SqlDbF, I](daoModule)

  lazy val controllerModule: ControllerModule[F, I] = new ControllerModuleImpl[F, SqlDbF, I](serviceModule, dbModule)
}

class Application[I[_]: Later: Monad, F[_]: Effect](config: ApplicationConfig) {
  import me.scf37.fpscala2.db.sql.db
  
  type SqlDbF[A] = SqlDb[F, A]

  lazy val commonModule: CommonModule[F, I] = new CommonModuleImpl[F, I](config.json)

  lazy val dbModule: DbModule[F, SqlDbF, I] = new DbModuleImpl[F, I](config.db)

  lazy val daoModule: DaoModule[SqlDbF, I] = new DaoModuleImpl[SqlDbF, I]

  lazy val serviceModule: ServiceModule[SqlDbF, I] = new ServiceModuleImpl[SqlDbF, I](daoModule)

  lazy val controllerModule: ControllerModule[F, I] = new ControllerModuleImpl[F, SqlDbF, I](serviceModule, dbModule)

  lazy val webModule: WebModule[F, I] = new WebModuleImpl[F, I](controllerModule, commonModule)

  lazy val serverModule: ServerModule[F, I] = new ServerModuleImpl[F, I](webModule, config.server)
}
