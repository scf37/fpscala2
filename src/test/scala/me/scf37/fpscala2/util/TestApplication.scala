package me.scf37.fpscala2.util

import cats.Parallel
import cats.data.Kleisli
import cats.effect.Async
import me.scf37.fpscala2.module.init.{Indi, IndiContext}
import me.scf37.fpscala2.Application
import org.scalatest.BeforeAndAfterAll

import java.sql.Connection
import java.util
import java.util.concurrent.atomic.AtomicInteger

/**
  * Reference counting hepler for integration tests.
  *
  * Given Application initialization effect will be initialized only once and closed when no one uses it anymore.
  *
  * @tparam F
  * @tparam DbEffect
  */
trait TestApplication[F[_]: Parallel: Async, DbEffect[_]](appI: Indi[F, Application[F, F, DbEffect]], eval: [A] => F[A] => A) extends BeforeAndAfterAll:
  self: org.scalatest.Suite =>

  import TestApplication._

  private lazy val (context, close) = eval(IndiContext[F].allocated)
  private var app0: Application[F, F, DbEffect] = _
  private val ref = new AtomicInteger(0)

  override def beforeAll(): Unit =
    values.synchronized {
      val (app0, st) = values.get(appI) match
        case null =>
          val (context, close) = eval(IndiContext[F].allocated)
          val value = eval(appI.evalOn(context))
          value -> AppState(1, value, () => eval(close))

        case st =>
          st.value -> st.copy(ref = st.ref + 1)

      values.put(appI, st)
      this.app0 = app0.asInstanceOf[Application[F, F, DbEffect]]
    }

    super.beforeAll()


  override def afterAll(): Unit =
    values.synchronized {
      val st = values.get(appI)
      if st.ref == 1 then
        values.remove(st)
        st.close()
      else
        values.put(appI, st.copy(ref = st.ref - 1))
    }

    super.afterAll()


  def app: Application[F, F, DbEffect] = app0
end TestApplication

object TestApplication:
  private val values = new util.IdentityHashMap[Any, AppState]()
  private case class AppState(
    ref: Int,
    value: Any,
    close: () => Unit
  )
