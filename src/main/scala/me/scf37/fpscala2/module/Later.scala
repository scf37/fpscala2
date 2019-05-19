package me.scf37.fpscala2.module

/**
  * Typeclass for monads supporting lazy evaluation and result memoization.
  * Both properties are required for initialization effect.
  *
  * @tparam I
  */
trait Later[I[_]] {
  /**
    * Lift f into I
    */
  def later[A](f: => A): I[A]

  /**
    * Mock I[A] so it will be evaluated to mock value
    */
  def setMock[A](i: I[A], mock: A): Unit
}

object Later {
  def apply[I[_]: Later]: Later[I] = implicitly[Later[I]]

  implicit val lazyInstance: Later[Lazy] = new Later[Lazy] {
    override def later[A](f: => A): Lazy[A] = Lazy(f)

    override def setMock[A](i: Lazy[A], mock: A): Unit = i.mock = Some(mock)
  }
}
