package me.scf37.fpscala2.psql

import me.scf37.fpscala2.module.config.DbConfig
import me.scf37.fpscala2.module.init.Init
import org.testcontainers.containers.PostgreSQLContainer

object EmbeddedPostgres:
  def apply[I[_], F[_]](using init: Init[I, F]): I[DbConfig] = init.delay {
    make()
  }

  private def make(): DbConfig =
    val postgres = new PostgreSQLContainer("postgres:11.1")
    postgres.start()

    val url = postgres.getJdbcUrl

    val user = postgres.getUsername
    val password = postgres.getPassword

    DbConfig(
      url = url,
      user = user,
      password = password,
      minPoolSize = 10,
      maxPoolSize = 10
    )
