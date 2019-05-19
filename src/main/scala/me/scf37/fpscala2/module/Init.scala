package me.scf37.fpscala2.module

class Init[A, F[_]](f: => F[A]) {

  lazy val value: F[A] = {
    over.getOrElse(f)
  }

  // override value of this Init monad
  @volatile
  var over: Option[F[A]] = None
}


