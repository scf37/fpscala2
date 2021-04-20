package me.scf37.fpscala2.memory

//import cats.Monad
//import me.scf37.fpscala2.dao.TodoDao
//import me.scf37.fpscala2.module.DaoModule
//import me.scf37.fpscala2.module.Later
//
//class MemoryDaoModule[I[_]: Later, F[_]: Monad] extends DaoModule[I, F] {
//  override val todoDao: I[TodoDao[F]] = Later[I].later {
//    new MemoryTodoDao[F]
//  }
//}
