package me.scf37.fpscala2.config

case class ApplicationConfig(
  json: JsonConfig,
  server: ServerConfig,
  db: DbConfig
)

object ApplicationConfig {
  val testConfig = ApplicationConfig(
    json = JsonConfig(
      pretty = false
    ),
    server = ServerConfig(
      interface = "localhost",
      port = 8080
    ),
    db = DbConfig(
      url = "",
      user = "",
      password = "",
      minPoolSize = 5,
      maxPoolSize = 10
    )
  )
}

