package me.scf37.fpscala2.int

import cats.Monad
import cats.Parallel
import cats.data.Kleisli
import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import cats.effect.Sync
import cats.effect.Async
import cats.effect.std.Dispatcher
import cats.implicits._
import me.scf37.fpscala2.Application
import me.scf37.fpscala2.db.sql.SqlEffectEval
import me.scf37.fpscala2.db.sql.SqlEffectLift
import me.scf37.fpscala2.module.DbModule
import me.scf37.fpscala2.module.config.ApplicationConfig
import me.scf37.fpscala2.module.config.DbConfig
import cats.effect.unsafe.implicits.global
import me.scf37.fpscala2.memory.{MemoryDaoModule, MemoryDbModule, MemoryTodoDaoState}
import me.scf37.fpscala2.module.init.{Indi, IndiContext, Init}
import me.scf37.fpscala2.psql.EmbeddedPostgres
import me.scf37.fpscala2.typeclass.Ask
import me.scf37.fpscala2.util.AsyncState

import java.sql.Connection

object IntegrationApp:

  object Acceptance:
    val app: Indi[IO, Application[IO, IO, Kleisli[IO, Connection, *]]] = Indi.applyWithContext { context =>
      Resource.eval(IO {
        withDatabase[Indi[IO, *], IO, Kleisli[IO, Connection, *]](
          db = EmbeddedPostgres[Indi[IO, *], IO],
          alwaysRollback = false
        ).mapK([A] => (e: Indi[IO, A]) => e.evalOn(context))
      })
    }

  object Postgres:
    val app: Indi[IO, Application[IO, IO, Kleisli[IO, Connection, *]]] = Indi.applyWithContext { context =>
      Resource.eval(IO {
        withDatabase[Indi[IO, *], IO, Kleisli[IO, Connection, *]](
          db = EmbeddedPostgres[Indi[IO, *], IO],
          alwaysRollback = true
        ).mapK([A] => (e: Indi[IO, A]) => e.evalOn(context))
      })
    }

  object Memory:
    type Eff[A] = Kleisli[IO, Ref[IO, TestState], A]
    def initialState: Ref[IO, TestState] = Ref.of[IO, TestState](TestState.Nil).unsafeRunSync()
    given testState: AsyncState[Eff, TestState] = AsyncState.make[Eff, IO, TestState]([A] => (io: IO[A]) => Kleisli.liftF(io))
    given testStateAsk: Ask[Eff, TestState] with
      def ask: Eff[TestState] = testState.get
    given daoState: AsyncState[Eff, MemoryTodoDaoState] = testState.extract(_.daoState, (st, daoState) => st.copy(daoState = daoState))

    val app: Indi[Eff, Application[Eff, Eff, Eff]] = Indi.applyWithContext { context =>
      Resource.eval(Kleisli liftF IO {
        withMemory[Indi[Eff, *], Eff].mapK([A] => (e: Indi[Eff, A]) => e.evalOn(context))
      })
    }

    case class TestState(
      daoState: MemoryTodoDaoState
    )
    object TestState {
      val Nil = TestState(MemoryTodoDaoState.Nil)
    }

  private def withMemory[I[_] :  Monad, F[_]: Async](
    using
    init: Init[I, F],
    st: AsyncState[F, MemoryTodoDaoState]
  ): Application[I, F, F] =
    val cfg = init.delay(ApplicationConfig.testConfig)

    // mock implementation of SQL effect (unused)
    implicit val sqlEff = new SqlEffectLift[F] with SqlEffectEval[F, F]:
      override def lift[A](f: Connection => F[A]): F[A] = f(null)
      override def eval[A](f: F[A], c: Connection): F[A] = f

    val app = Application[I, F, F](
      cfg,
      withDbModule = _ => MemoryDbModule[I, F],
      withDaoModule = _ => MemoryDaoModule[I, F]
    )

    app

  private def withDatabase[I[_]:  Monad, F[_]: Async, DbEffect[_] : Sync](
    db: I[DbConfig],
    alwaysRollback: Boolean
  )(
    implicit
    DB: SqlEffectLift[DbEffect],
    DE: SqlEffectEval[F, DbEffect],
    init: Init[I, F]
  ): Application[I, F, DbEffect] =
    val cfg = db.map(db => ApplicationConfig.testConfig.copy(db = db))

    val app = Application[I, F, DbEffect](
      cfg,
      withDbModule = _ => DbModule[I, F, DbEffect](cfg.map(_.db), alwaysRollback = alwaysRollback)
    )

    app
