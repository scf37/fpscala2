package me.scf37.fpscala2.module.init

import cats.effect.Resource
import cats.effect.Deferred
import cats.effect.Ref
import cats.implicits._
import cats.effect.Async

trait LoadingCache[F[_], K[+_]]:
  /**
    * Get value from this cache.
    *
    * This is loading cache, i.e. if value is not in the cache yet, `load` will be used to
    * evaluate value of A
    *
    * Returned outer effect represents memoization, inner effect represents value evaluation
    *
    * @param key
    * @param load
    * @tparam A
    * @return
    */
  def get[A](
    key: K[A],
    load: Resource[F, A]
  ): F[F[A]]


object LoadingCache:
  def apply[F[_]: Async, K[+_]]: Resource[F, LoadingCache[F, K]] = Resource {
    for
      ref <- Ref.of(State[F, K](Map.empty, Map.empty, ().pure))
      impl = Impl[F, K](ref)
    yield impl -> impl.close()
  }

  private type Value = Any
  private case class State[F[_], K[_]](
    // cache contents
    values: Map[K[Value], Value],
    // values that are still loading
    loading: Map[K[Value], Deferred[F, F[F[Value]]]],
    // effect to deallocate all cached resources
    close: F[Unit]
  )

  private case class Impl[F[_]: Async, K[+_]](ref: Ref[F, State[F, K]]) extends LoadingCache[F, K]:
    override def get[A](
      key: K[A],
      load: Resource[F, A]
    ): F[F[A]] =
      Deferred[F, F[F[Value]]].flatMap { d =>
        ref.modify { state =>
          handleGet(
            key = key,
            load = load,
            state = state,
            d = d
          )
        }.flatten
      }
    end get

    private def handleGet[A](
      key: K[A],
      load: Resource[F, A],
      state: State[F, K],
      d: Deferred[F, F[F[Value]]]
    ): (State[F, K], F[F[A]]) =
      // already loading? return existing deferred
      state.loading.get(key) match
        case Some(value) => return state -> value.get.map(_.map(_.map(_.asInstanceOf[A]))).flatten
        case None =>

      // already cached? return cached value
      state.values.get(key) match
        case Some(value) => return state -> value.asInstanceOf[A].pure.pure
        case None =>

      val nextState = state.copy(
        loading = state.loading + (key -> d)
      )

      nextState -> Async[F].memoize(load.allocated).map { allocated =>
        allocated.flatMap { case (value, close) =>
          for
            _ <- this.ref.update(st =>
              st.copy(
                close = close.attempt >> st.close,
                values = st.values + (key -> value),
                loading = st.loading - key
              )
            )
            _ <- d.complete(value.pure.pure)
          yield value
        }
      }
    end handleGet

    def close(): F[Unit] = ref.get.flatMap(_.close)

  end Impl