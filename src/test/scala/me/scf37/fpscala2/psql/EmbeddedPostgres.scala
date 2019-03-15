package me.scf37.fpscala2.psql

import me.scf37.fpscala2.module.config.DbConfig
import ru.yandex.qatools.embed.postgresql
import ru.yandex.qatools.embed.postgresql.distribution.Version

object EmbeddedPostgres {
  lazy val instance = make()
  lazy val acceptanceInstance = make()

  private def make(): DbConfig = {
    val postgres = new postgresql.EmbeddedPostgres(Version.V9_6_3)

    val url = {
      val url: String = postgres.start()
      Runtime.getRuntime.addShutdownHook(new Thread(() => postgres.stop()))
      url
    }

    val user = postgresql.EmbeddedPostgres.DEFAULT_USER
    val password = postgresql.EmbeddedPostgres.DEFAULT_PASSWORD

    DbConfig(
      url = url,
      user = user,
      password = password,
      minPoolSize = 10,
      maxPoolSize = 10
    )
  }
}
