package me.scf37.fpscala2.dao

import cats.effect.IO
import me.scf37.fpscala2.db.sql.SqlEffect
import me.scf37.fpscala2.int.IntegrationTest
import me.scf37.fpscala2.model.Todo

class TodoDaoTest extends IntegrationTest {
  val dao = app.daoModule.todoDao.value.right.get

  "basic CRUD" in db {
    for {
      list <- dao.list()
      _ = assert(list.isEmpty)

      added <- dao.save(Todo("1", "text1"))
      _ = assert(added.id == "1")
      _ = assert(added.text == "text1")

      list1 <- dao.list()
      _ = assert(list1.size == 1)

      readOpt <- dao.get("1")
      _ = assert(readOpt.isDefined)
      _ = assert(readOpt.get.id == "1")
      _ = assert(readOpt.get.text == "text1")

      updated <- dao.save(Todo("1", "text2"))
      _ = assert(updated.id == "1")
      _ = assert(updated.text == "text2")

      read2Opt <- dao.get("1")
      _ = assert(read2Opt.isDefined)
      _ = assert(read2Opt.get.id == "1")
      _ = assert(read2Opt.get.text == "text2")

      deleted <- dao.delete("1")
      _ = assert(deleted)
      readDeletedOpt <- dao.get("1")
      _ = assert(readDeletedOpt.isEmpty)
    } yield ()
  }

  "saving two items" in db {
    for {
      _ <- dao.save(Todo("1", "test1"))
      _ <- dao.save(Todo("2", "test2"))

      list <- dao.list()
      _ = assert(list.map(_.id).toSet == Set("1", "2"))

      i1 <- dao.get("1")
      i2 <- dao.get("2")
      _ = assert(i1.get.text == "test1")
      _ = assert(i2.get.text == "test2")
    } yield ()
  }

  private def db[A](value: SqlEffect[IO, A]): Unit = {
    app.dbModule.tx.value.right.get.tx(value).unsafeRunSync()
  }
}
