package me.scf37.fpscala2.module.config

import com.typesafe.config.Config

case class ServerConfig(
  interface: String,
  port: Int
)

object ServerConfig:
  def parse(c: Config): ServerConfig = ServerConfig(
    interface = c.getString("interface"),
    port = c.getInt("port")
  )
