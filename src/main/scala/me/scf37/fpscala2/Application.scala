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
import me.scf37.fpscala2.module.ServerModule
import me.scf37.fpscala2.module.ServiceModule
import me.scf37.fpscala2.module.WebModule
import me.scf37.fpscala2.module.config.ApplicationConfig
import cats.effect.std.Dispatcher
import me.scf37.fpscala2.module.init.Init

/**
  * assembled application together with all dependencies
  *
  * @tparam I initialization effect - evaluation initializes relevant classes
  * @tparam F working effect
  * @tparam DbEffect database effect
  */
case class Application[I[_], F[_], DbEffect[_]](
  commonModule: CommonModule[I, F],
  dbModule: DbModule[I, F, DbEffect],
  daoModule: DaoModule[I, DbEffect],
  serviceModule: ServiceModule[I, DbEffect],
  controllerModule: ControllerModule[I, F],
  webModule: WebModule[I, F],
  serverModule: ServerModule[I, F]
):
  /**
    * Map over initialization effect.
    * Used to convert stateless Application[I, F, DbEffect] to Application[F, F, DbEffect] ready to be used
    * @param f
    * @tparam II
    * @return
    */
  def mapK[II[_]](f: [A] => I[A] => II[A]): Application[II, F, DbEffect] = Application(
    commonModule = commonModule.mapK(f),
    dbModule = dbModule.mapK(f),
    daoModule = daoModule.mapK(f),
    serviceModule = serviceModule.mapK(f),
    controllerModule = controllerModule.mapK(f),
    webModule = webModule.mapK(f),
    serverModule = serverModule.mapK(f)
  )


object Application:

  def apply[I[_]: Monad, F[_]: Async, DbEffect[_]: MonadThrow](
    config: I[ApplicationConfig],
    withDbModule: DbModule[I, F, DbEffect] => DbModule[I, F, DbEffect] = identity[DbModule[I, F, DbEffect]],
    withDaoModule: DaoModule[I, DbEffect] => DaoModule[I, DbEffect] = identity[DaoModule[I, DbEffect]]
  )(
    using
    init: Init[I, F],
    DB: SqlEffectLift[DbEffect],
    DE: SqlEffectEval[F, DbEffect]
  ): Application[I, F, DbEffect] =

    val commonModule: CommonModule[I, F] = CommonModule[I, F]

    val dbModule: DbModule[I, F, DbEffect] =
      withDbModule(DbModule[I, F, DbEffect](config.map(_.db), false))

    val daoModule: DaoModule[I, DbEffect] = withDaoModule(DaoModule[I, F, DbEffect])

    val serviceModule: ServiceModule[I, DbEffect] = ServiceModule[I, F, DbEffect](daoModule)

    val controllerModule: ControllerModule[I, F] = ControllerModule[I, F, DbEffect](
      commonModule, serviceModule, dbModule)

    val webModule: WebModule[I, F] = WebModule[I, F](controllerModule, commonModule)

    val serverModule: ServerModule[I, F] = ServerModule[I, F](
      webModule, commonModule, config.map(_.server))

    Application[I, F, DbEffect](
      commonModule = commonModule,
      dbModule = dbModule,
      daoModule = daoModule,
      serviceModule = serviceModule,
      controllerModule = controllerModule,
      webModule = webModule,
      serverModule = serverModule
    )
