package me.scf37.fpscala2

import cats.Eval
import cats.effect.IO
import me.scf37.fpscala2.config.ApplicationConfig
import me.scf37.fpscala2.config.DbConfig
import me.scf37.fpscala2.config.JsonConfig
import me.scf37.fpscala2.config.ServerConfig


object Main {
  def main(args: Array[String]): Unit = {

    val config: ApplicationConfig = ApplicationConfig(
      json = JsonConfig(
        pretty = false
      ),
      server = ServerConfig(
        interface = "localhost",
        port = 8080
      ),
      db = DbConfig(
        url = "jdbc:postgresql://localhost:5432/fpscala2",
        user = "postgres",
        password = "",
        minPoolSize = 5,
        maxPoolSize = 10
      )
    )

    val app = new Application[Eval, IO](config)

    val server = app.serverModule.server.value

    server()

  }
}
