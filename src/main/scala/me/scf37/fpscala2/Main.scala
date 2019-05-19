package me.scf37.fpscala2

import cats.Applicative
import cats.Defer
import cats.data.EitherT
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Sync
import cats.implicits._
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import me.scf37.config3.Config3
import me.scf37.config3.Config3.PrintedConfig
import me.scf37.fpscala2.db.sql.SqlEffect
import me.scf37.fpscala2.module.Lazy
import me.scf37.fpscala2.module.config.ApplicationConfig

object Main extends IOApp {

  private def env[F[_]: Applicative: Defer]: F[Option[String]] = Defer[F].defer {
    Option(System.getProperty("env"))
      .orElse(Option(System.getenv("env"))).pure[F]
  }

  override def run(args: List[String]): IO[ExitCode] = {

    val startedApp: EitherT[IO, EagerExit, Unit] = for {
      envOpt <- env[EitherT[IO, EagerExit, ?]]

      configInfo <- loadConfig[IO](args.toArray, envOpt.map(_ + ".conf"))
      (printedConfig, config) = configInfo

      _ <- EitherT.right(IO(println(printedConfig.toString)))

      appConfig = ApplicationConfig.load(config)

      app = new Application[Lazy, IO, SqlEffect[IO, ?]](appConfig)
      server <- EitherT.right(IO.fromEither(app.serverModule.value.flatMap(_.server.value)))
      _ <- EitherT.right(IO(server()))
    } yield ()

    startedApp.value.flatMap {

      case Left(eagerExit) =>
        IO(println(eagerExit.message)).map(_ => eagerExit.code)

      case Right(_) => IO(ExitCode(0))
    }

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
  ): EitherT[F, EagerExit, (PrintedConfig, Config)] = {

    def isAppConfigKey(key: String) = key.startsWith("app.")
    def isPasswordKey(key: String) = key.contains("password")

    for {
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

    } yield printedConfig -> config
  }
}
