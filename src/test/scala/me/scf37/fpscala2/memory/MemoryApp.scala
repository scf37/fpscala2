package me.scf37.fpscala2.memory

import cats.Monad
import cats.effect.Effect
import me.scf37.fpscala2.ApplicationBase
import me.scf37.fpscala2.module.DaoModule
import me.scf37.fpscala2.module.DbModule
import me.scf37.fpscala2.module.Later
import me.scf37.fpscala2.module.config.ApplicationConfig

class MemoryApp[I[_]: Later: Monad, F[_]: Effect](
) extends ApplicationBase[I, F, F](ApplicationConfig.testConfig) {

  override lazy val daoModule: DaoModule[F, I] = new MemoryDaoModule[F, I]

  override lazy val dbModule: DbModule[F, F, I] = new MemoryDbModule[F, I]
}
