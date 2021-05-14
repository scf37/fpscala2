package me.scf37.fpscala2.dao

import org.scalatest.BeforeAndAfterAll
import cats.effect.IO
import cats.data.Kleisli
import me.scf37.fpscala2.int.IntegrationApp
import cats.effect.unsafe.implicits.global
import me.scf37.fpscala2.Application
import me.scf37.fpscala2.util.TestApplication

import java.sql.Connection

// TodoDao test using embedded postgres
class DbTodoDaoTest extends TodoDaoTest[IO, Kleisli[IO, Connection, *]]([A] => (io: IO[A]) => io.unsafeRunSync())
  with TestApplication[IO, Kleisli[IO, Connection, *]](IntegrationApp.Postgres.app, [A] => (io: IO[A]) => io.unsafeRunSync())