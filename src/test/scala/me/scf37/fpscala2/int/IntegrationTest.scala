package me.scf37.fpscala2.int

import cats.Eval
import cats.effect.IO
import me.scf37.fpscala2.psql.EmbeddedPostgres
import org.scalatest.FreeSpec

abstract class IntegrationTest extends FreeSpec {
  lazy val app = IntegrationTest.app
}

object IntegrationTest {
  import me.scf37.fpscala2.db.sql._
  lazy val app = IntegrationApp.make[Eval, IO, SqlEffect[IO, ?]](EmbeddedPostgres.instance)
}
