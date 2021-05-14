package me.scf37.fpscala2.module.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

case class ApplicationConfig(
  server: ServerConfig,
  db: DbConfig
)

object ApplicationConfig:

  def parse(c: Config): ApplicationConfig = ApplicationConfig(
    server = ServerConfig.parse(c.getConfig("app.server")),
    db = DbConfig.parse(c.getConfig("app.db"))
  )

  val testConfig: ApplicationConfig = parse(ConfigFactory.load("local"))
