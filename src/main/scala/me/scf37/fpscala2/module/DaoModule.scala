package me.scf37.fpscala2.module

import cats.Monad
import cats.effect.Sync
import me.scf37.fpscala2.dao.TodoDao
import me.scf37.fpscala2.dao.sql.TodoDaoSql
import me.scf37.fpscala2.db.sql.SqlEffectLift

trait DaoModule[I[_], DbEffect[_]] {
  def todoDao: I[TodoDao[DbEffect]]
}

class DaoModuleImpl[I[_]: Later: Monad, F[_]: Sync, DbEffect[_]: Monad](
  implicit DB: SqlEffectLift[F, DbEffect]
) extends DaoModule[I, DbEffect] {

  override lazy val todoDao: I[TodoDao[DbEffect]] = Later[I].later {
    new TodoDaoSql[F, DbEffect]
  }
}
