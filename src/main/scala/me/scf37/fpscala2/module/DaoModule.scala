package me.scf37.fpscala2.module

import cats.Monad
import cats.effect.Sync
import me.scf37.fpscala2.dao.TodoDao
import me.scf37.fpscala2.dao.sql.TodoDaoSql
import me.scf37.fpscala2.db.sql.SqlEffectLift

trait DaoModule[DbEffect[_], I[_]] {
  def todoDao: I[TodoDao[DbEffect]]
}

class DaoModuleImpl[DbEffect[_]: Monad, F[_]: Sync, I[_]: Later: Monad](
  implicit DB: SqlEffectLift[DbEffect, F]
) extends DaoModule[DbEffect, I] {

  override lazy val todoDao: I[TodoDao[DbEffect]] = Later[I].later {
    new TodoDaoSql[DbEffect, F]
  }
}
