package me.scf37.fpscala2.memory

//import cats.arrow.FunctionK
//import cats.~>
//import me.scf37.fpscala2.db.TxManager
//import me.scf37.fpscala2.module.DbModule
//import me.scf37.fpscala2.module.Later
//
//class MemoryDbModule[I[_]: Later, F[_]] extends DbModule[I, F, F] {
//
//  override val tx: I[TxManager[F, F]] = Later[I].later {
//    new TxManager[F, F] {
//      override def tx: F ~> F = FunctionK.lift(impl)
//
//      private def impl[A](t: F[A]): F[A] = t
//    }
//  }
//}
