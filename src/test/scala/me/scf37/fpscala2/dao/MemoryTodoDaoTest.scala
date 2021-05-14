package me.scf37.fpscala2.dao

import cats.effect.IO
import cats.effect.std.Dispatcher
import me.scf37.fpscala2.module.init.{Indi, IndiContext}
import cats.effect.unsafe.implicits.global
import me.scf37.fpscala2.Application
import me.scf37.fpscala2.int.IntegrationApp
import me.scf37.fpscala2.util.TestApplication
import org.scalatest.BeforeAndAfterAll
import IntegrationApp.Memory.Eff
import IntegrationApp.Memory

// TodoDao test using in-memory dao implementation
class MemoryTodoDaoTest extends TodoDaoTest[Eff, Eff]([A] => (f: Eff[A]) => f.run(Memory.initialState).unsafeRunSync())
  with TestApplication[Eff, Eff](IntegrationApp.Memory.app, [A] => (f: Eff[A]) => f.run(Memory.initialState).unsafeRunSync())