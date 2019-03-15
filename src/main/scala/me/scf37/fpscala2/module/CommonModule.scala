package me.scf37.fpscala2.module

import cats.Monad
import cats.effect.Sync
import com.fasterxml.jackson.databind.SerializationFeature
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.modules.FinatraJacksonModule
import me.scf37.fpscala2.logging.Log
import me.scf37.fpscala2.logging.LogImpl
import me.scf37.fpscala2.module.config.JsonConfig

trait CommonModule[F[_], I[_]] {
  def json: I[FinatraObjectMapper]

  def log: I[Log[F]]
}

class CommonModuleImpl[F[_]: Sync, I[_]: Later: Monad](
  jsonConfig: JsonConfig
) extends CommonModule[F, I] {

  override lazy val json: I[FinatraObjectMapper] = Later[I].later {
    val om = FinatraJacksonModule.provideScalaObjectMapper(null)
    if (jsonConfig.pretty) {
      om.configure(SerializationFeature.INDENT_OUTPUT, true)
    }
    FinatraJacksonModule.provideCamelCaseFinatraObjectMapper(om)
  }

  override lazy val log: I[Log[F]] = Later[I].later {
    new LogImpl[F]
  }
}