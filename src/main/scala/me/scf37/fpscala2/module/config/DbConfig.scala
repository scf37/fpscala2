package me.scf37.fpscala2.module.config

import com.typesafe.config.Config

case class DbConfig(
  url: String,
  user: String,
  password: String,
  minPoolSize: Int,
  maxPoolSize: Int
)

object DbConfig:
  def parse(c: Config): DbConfig = DbConfig(
    url = c.getString("url"),
    user = c.getString("user"),
    password = c.getString("password"),
    minPoolSize = c.getInt("minPoolSize"),
    maxPoolSize = c.getInt("maxPoolSize")
  )