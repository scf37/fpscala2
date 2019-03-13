package me.scf37.fpscala2.config.module

import cats.Monad
import me.scf37.fpscala2.config.Later
import me.scf37.fpscala2.dao.TodoDao
import me.scf37.fpscala2.dao.sql.TodoDaoSql
import me.scf37.fpscala2.db.Db

trait DaoModule[F[_], I[_]] {
  def todoDao: I[TodoDao[F]]
}

class DaoModuleImpl[F[_]: Db: Monad, I[_]: Later: Monad] extends DaoModule[F, I] {

  override lazy val todoDao: I[TodoDao[F]] = Later[I].later {
    new TodoDaoSql[F]
  }
}
