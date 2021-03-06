package me.scf37.fpscala2

import cats.Applicative
import cats.Defer
import cats.data.EitherT
import cats.data.Kleisli
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Sync
import cats.effect.std.Dispatcher
import cats.implicits._
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import me.scf37.config3.Config3
import me.scf37.config3.Config3.PrintedConfig
import me.scf37.fpscala2.module.config.ApplicationConfig
import me.scf37.fpscala2.module.init.{Indi, IndiContext}

import java.sql.Connection

object Main extends IOApp:

  private def env[F[_]: Sync]: F[Option[String]] = Sync[F].delay {
    Option(System.getProperty("env"))
      .orElse(Option(System.getenv("env")))
  }

  override def run(args: List[String]): IO[ExitCode] =
    import cats.effect.unsafe.implicits.global

    val startedApp: EitherT[IO, EagerExit, Unit] = for
      envOpt <- env[EitherT[IO, EagerExit, *]]

      configInfo <- loadConfig[IO](args.toArray, envOpt.map(_ + ".conf"))
      (printedConfig, config) = configInfo

      _ <- EitherT.right(IO(println(printedConfig.toString)))

      appConfig = Indi.delay[IO, ApplicationConfig](ApplicationConfig.parse(config))
      app = Application[Indi[IO, *], IO, Kleisli[IO, Connection, *]](appConfig)

      result = IndiContext[IO].use { ctx =>
        app.serverModule.server.evalOn(ctx).map(_())
      }
      _ <- EitherT.right(result)
    yield ()

    startedApp.value.flatMap {

      case Left(eagerExit) =>
        IO(println(eagerExit.message)).map(_ => eagerExit.code)

      case Right(_) => IO(ExitCode(0))
    }


  private case class EagerExit(message: String, code: ExitCode)

  /**
    * Load configuration from multiple sources or fail with error message or exit code
    *
    * @param args command line arguments
    * @param configFile configuration file (optional)
    * @tparam F
    * @return
    */
  private def loadConfig[F[_]: Sync](
    args: Array[String],
    configFile: Option[String]
  ): EitherT[F, EagerExit, (PrintedConfig, Config)] =

    def isAppConfigKey(key: String) = key.startsWith("app.")
    def isPasswordKey(key: String) = key.contains("password")

    for
      reference <- EitherT.pure[F, EagerExit](ConfigFactory.parseResources("reference.conf"))

      _ <- EitherT.cond[F](!args.sameElements(Seq("--help")), (),
        EagerExit(Config3.help(reference, isAppConfigKey).toString(), ExitCode(1))
      )

      cmdlineConfig <- EitherT.fromEither[F](
        Config3.parse(args).left.map(error => EagerExit(error.toString(), ExitCode(2)))
      )

      config = cmdlineConfig
        .withFallback(ConfigFactory.systemProperties())
        .withFallback(ConfigFactory.systemEnvironment())
        .withFallback(configFile.fold(ConfigFactory.empty)(ConfigFactory.load))
        .withFallback(reference)

      errors = Config3.validate(reference, config, isAppConfigKey)

      _ <- EitherT.cond[F](errors.isEmpty, (), EagerExit(errors.mkString("\n"), ExitCode(1)))

      printedConfig = Config3.printConfig(reference, config, isAppConfigKey, isPasswordKey)

    yield printedConfig -> config
