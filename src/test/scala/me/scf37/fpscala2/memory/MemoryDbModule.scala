package me.scf37.fpscala2.memory

import cats.arrow.FunctionK
import cats.~>
import me.scf37.fpscala2.db.TxManager
import me.scf37.fpscala2.module.init.Init
import me.scf37.fpscala2.module.DbModule

object MemoryDbModule:

  def apply[I[_], F[_]](using init: Init[I, F]): DbModule[I, F, F] =
    DbModule(
      tx = init.delay {
        new TxManager[F, F]:
          override def tx: [A] => F[A] => F[A] = [A] => (x: F[A]) => x

      }
    )
