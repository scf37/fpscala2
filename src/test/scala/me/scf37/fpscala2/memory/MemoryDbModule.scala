package me.scf37.fpscala2.memory

import cats.arrow.FunctionK
import cats.~>
import me.scf37.fpscala2.module.Later
import me.scf37.fpscala2.module.DbModule
import me.scf37.fpscala2.db.TxManager

class MemoryDbModule[F[_], I[_]: Later] extends DbModule[F, F, I] {

  override lazy val tx: I[TxManager[F, F]] = Later[I].later {
    new TxManager[F, F] {
      override def tx: F ~> F = FunctionK.lift(impl)

      private def impl[A](t: F[A]): F[A] = t
    }
  }
}
