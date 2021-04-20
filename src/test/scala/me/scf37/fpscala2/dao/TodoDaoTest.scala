package me.scf37.fpscala2.dao

//import cats.effect.Effect
//import cats.effect.Sync
//import cats.implicits._
//import me.scf37.fpscala2.Application
//import me.scf37.fpscala2.db.TxManager
//import me.scf37.fpscala2.model.Todo
//import me.scf37.fpscala2.module.Lazy
//import org.scalatest.FreeSpec
//
//abstract class TodoDaoTest[F[_]: Effect, DbEffect[_]: Sync] extends FreeSpec {
//
//  protected def app: Application[Lazy, F, DbEffect]
//
//  private val dao: TodoDao[DbEffect] =
//    app.daoModule.value.flatMap(_.todoDao.value).right.get
//
//  private val txManager: TxManager[F, DbEffect] =
//    app.dbModule.value.flatMap(_.tx.value).right.get
//
//
//  "basic CRUD" in db {
//    for {
//      list <- dao.list()
//      _ = assert(list.isEmpty)
//
//      added <- dao.save(Todo("1", "text1"))
//      _ = assert(added.id == "1")
//      _ = assert(added.text == "text1")
//
//      list1 <- dao.list()
//      _ = assert(list1.size == 1)
//
//      readOpt <- dao.get("1")
//      _ = assert(readOpt.isDefined)
//      _ = assert(readOpt.get.id == "1")
//      _ = assert(readOpt.get.text == "text1")
//
//      updated <- dao.save(Todo("1", "text2"))
//      _ = assert(updated.id == "1")
//      _ = assert(updated.text == "text2")
//
//      read2Opt <- dao.get("1")
//      _ = assert(read2Opt.isDefined)
//      _ = assert(read2Opt.get.id == "1")
//      _ = assert(read2Opt.get.text == "text2")
//
//      deleted <- dao.delete("1")
//      _ = assert(deleted)
//      readDeletedOpt <- dao.get("1")
//      _ = assert(readDeletedOpt.isEmpty)
//    } yield ()
//  }
//
//  "saving two items" in db {
//    for {
//      _ <- dao.save(Todo("1", "test1"))
//      _ <- dao.save(Todo("2", "test2"))
//
//      list <- dao.list()
//      _ = assert(list.map(_.id).toSet == Set("1", "2"))
//
//      i1 <- dao.get("1")
//      i2 <- dao.get("2")
//      _ = assert(i1.get.text == "test1")
//      _ = assert(i2.get.text == "test2")
//    } yield ()
//  }
//
//  private def db[A](value: DbEffect[A]): Unit = {
//    Effect[F].toIO(txManager.tx(value)).unsafeRunSync()
//  }
//}
