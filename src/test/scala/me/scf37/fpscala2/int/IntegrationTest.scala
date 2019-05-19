package me.scf37.fpscala2.int

import cats.effect.IO
import me.scf37.fpscala2.module.Lazy
import me.scf37.fpscala2.psql.EmbeddedPostgres
import org.scalatest.FreeSpec

trait IntegrationTest extends FreeSpec {
  lazy val app = IntegrationTest.app
}

object IntegrationTest {
  import me.scf37.fpscala2.db.sql._
  lazy val app = IntegrationApp[Lazy, IO, SqlEffect[IO, ?]](
    db = EmbeddedPostgres.instance,
    alwaysRollback = true
  )
}
