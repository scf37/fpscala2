package me.scf37.fpscala2

import cats.Eval
import cats.effect.IO
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import me.scf37.config3.Config3
import me.scf37.fpscala2.module.config.ApplicationConfig

object Main {
  private val env = Option(System.getProperty("env"))
    .orElse(Option(System.getenv("env")))
    .getOrElse("")

  def main(args: Array[String]): Unit = {

    val config: ApplicationConfig = ApplicationConfig.load(loadConfig(args, env))

    import me.scf37.fpscala2.db.sql._
    val app = new Application[Eval, IO, SqlEffect[IO, ?]](config)

    val server = app.serverModule.server.value

    server()
  }

  private def loadConfig(args: Array[String], env: String): Config = {
    val reference = ConfigFactory.parseResources("reference.conf")
    def isAppConfigKey(key: String) = key.startsWith("app.")

    if (args.sameElements(Seq("--help"))) {
      println(Config3.help(reference, isAppConfigKey))
      System.exit(1)
    }

    val cmdlineConfig = Config3.parse(args) match {
      case Left(error) =>
        println(error)
        System.exit(2)
        ???
      case Right(config) => config
    }

    val config =
      cmdlineConfig
        .withFallback(ConfigFactory.systemProperties())
        .withFallback(ConfigFactory.systemEnvironment())
        .withFallback(ConfigFactory.load(s"$env.conf"))
        .withFallback(reference)

    val errors = Config3.validate(reference, config, isAppConfigKey)
    if (errors.nonEmpty) {
      errors.foreach(println)
      System.exit(2)
    }

    // print resolved configuration
    println(Config3.printConfig(reference, config, isAppConfigKey, _.contains("password")))

    config
  }
}
