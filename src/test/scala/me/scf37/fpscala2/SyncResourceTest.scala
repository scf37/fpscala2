package me.scf37.fpscala2

import cats.MonadThrow
import cats.implicits._
import me.scf37.fpscala2.typeclass.SyncResource
import org.scalatest.freespec.AnyFreeSpec

import scala.collection.mutable

class SyncResourceTest extends AnyFreeSpec:
  "SyncResource correctly evaluates and closes resource" - {
    "on success" in {
      def test[F[_] : MonadThrow]: F[Unit] =
        val closeOrder = mutable.Buffer.empty[String]
        val c1 = new TestCloseable("c1", closeOrder)
        val c2 = new TestCloseable("c2", closeOrder)
        val resource =
          for
            _ <- SyncResource.fromAutoCloseable(c1.init())
            _ <- SyncResource.fromAutoCloseable(c2.init())
            _ = assert(c1.currentThread == c2.currentThread)
            _ = assert(c1.initCount == 1)
            _ = assert(c1.closeCount == 0)
            _ = assert(c2.initCount == 1)
            _ = assert(c2.closeCount == 0)
          yield ()

        // SyncResource is lazy
        assert(c1.initCount == 0)
        assert(c1.closeCount == 0)
        assert(c2.initCount == 0)
        assert(c2.closeCount == 0)

        resource.use(r => {
          // resource is initialized and ready to use
          assert(c1.initCount == 1)
          assert(c1.closeCount == 0)
          assert(c2.initCount == 1)
          assert(c2.closeCount == 0)
          ().pure[F]
        }).map { _ =>
          // resource is correctly closed
          assert(c1.initCount == 1)
          assert(c1.closeCount == 1)
          assert(c2.initCount == 1)
          assert(c2.closeCount == 1)
          assert(closeOrder == Vector("c2", "c1"))
          ()
        }
      end test

      test[Either[Throwable, *]]
      import cats.effect.unsafe.implicits.global
      cats.effect.IO(()).map(_ => test[cats.effect.IO]).unsafeRunSync()
    }
    "when use throws exception" - {
      "directly" in {
        def test[F[_] : MonadThrow]: F[Unit] =
          val closeOrder = mutable.Buffer.empty[String]
          val c1 = new TestCloseable("c1", closeOrder)
          val c2 = new TestCloseable("c2", closeOrder)
          val resource =
            for
              _ <- SyncResource.fromAutoCloseable(c1.init())
              _ <- SyncResource.fromAutoCloseable(c2.init())
            yield ()

          resource.use(r => {
            throw new RuntimeException
          }).map { _ =>
            // resource is correctly closed
            assert(c1.initCount == 1)
            assert(c1.closeCount == 1)
            assert(c2.initCount == 1)
            assert(c2.closeCount == 1)
            assert(closeOrder == Vector("c2", "c1"))
            ()
          }
        end test

        test[Either[Throwable, *]]
        import cats.effect.unsafe.implicits.global
        cats.effect.IO(()).map(_ => test[cats.effect.IO]).unsafeRunSync()
      }

      "using raiseError" in {
        def test[F[_] : MonadThrow]: F[Unit] =
          val closeOrder = mutable.Buffer.empty[String]
          val c1 = new TestCloseable("c1", closeOrder)
          val c2 = new TestCloseable("c2", closeOrder)
          val resource =
            for
              _ <- SyncResource.fromAutoCloseable(c1.init())
              _ <- SyncResource.fromAutoCloseable(c2.init())
            yield ()

          resource.use(r => {
            (new RuntimeException()).raiseError
          }).map { _ =>
            // resource is correctly closed
            assert(c1.initCount == 1)
            assert(c1.closeCount == 1)
            assert(c2.initCount == 1)
            assert(c2.closeCount == 1)
            assert(closeOrder == Vector("c2", "c1"))
            ()
          }
        end test

        test[Either[Throwable, *]]
        import cats.effect.unsafe.implicits.global
        cats.effect.IO(()).map(_ => test[cats.effect.IO]).unsafeRunSync()
      }
    }

    "when close throws exception" in {
      def test[F[_] : MonadThrow]: F[Unit] =
        val closeOrder = mutable.Buffer.empty[String]
        val c1 = new TestCloseable("c1", closeOrder)
        val c2 = new TestCloseable("c2", closeOrder)
        val resource =
          for
            _ <- SyncResource.fromAutoCloseable(c1.init(false))
            _ <- SyncResource.fromAutoCloseable(c2.init(true))
          yield ()

        resource.use(r => {
          ().pure[F]
        }).map { _ =>
          // resource is correctly closed
          assert(c1.initCount == 1, "a")
          assert(c1.closeCount == 1, "aa")
          assert(c2.initCount == 1)
          assert(c2.closeCount == 1)
          assert(closeOrder == Vector("c2", "c1"))
          ()
        }
      end test

      test[Either[Throwable, *]]
      import cats.effect.unsafe.implicits.global
      cats.effect.IO(()).map(_ => test[cats.effect.IO]).unsafeRunSync()
    }

    "when init throws exception" in {
      def test[F[_] : MonadThrow]: F[Unit] =
        val closeOrder = mutable.Buffer.empty[String]
        val c1 = new TestCloseable("c1", closeOrder)
        val c2 = new TestCloseable("c2", closeOrder)
        val resource =
          for
            _ <- SyncResource.fromAutoCloseable(c1.init())
            _ <- SyncResource.fromAutoCloseable(throw new RuntimeException)
          yield ()
        val r =
          try
            resource.use(r => {
              ().pure[F]
            }).attempt.void
          catch
            case e: RuntimeException => ().pure

        r.map { _ =>
          // resource is correctly closed
          assert(c1.initCount == 1)
          assert(c1.closeCount == 1, "c1 close")
          assert(c2.initCount == 0)
          assert(c2.closeCount == 0)
          assert(closeOrder == Vector("c1"))
          ()
        }
      end test

      test[Either[Throwable, *]]
      import cats.effect.unsafe.implicits.global
      cats.effect.IO(()).map(_ => test[cats.effect.IO]).unsafeRunSync()
    }
  }

  private def fp(f: [F[_]] => MonadThrow[F] ?=> () => F[Unit]): Unit =
    f[Either[Throwable, *]]
    import cats.effect.unsafe.implicits.global
    cats.effect.IO(()).map(_ => f[cats.effect.IO]).unsafeRunSync()

  // https://github.com/lampepfl/dotty/issues/12274
  // otherwise fp { [F[_]] => () => {
  // after https://github.com/lampepfl/dotty/issues/12277
  // even fp { [F[_]] => () =>
  // and if/when scala 3 supports by-name params in polymorphic functions
  // fp { [F[_]] =>
  "example test" in fp { [F[_]] => (ev: MonadThrow[F]) ?=> () => {
    val closeOrder = mutable.Buffer.empty[String]
    val c1 = new TestCloseable("c1", closeOrder)
    val c2 = new TestCloseable("c2", closeOrder)
    val resource =
      for
        _ <- SyncResource.fromAutoCloseable(c1.init())
        _ <- SyncResource.fromAutoCloseable(throw new RuntimeException)
      yield ()
    val r =
      try
        resource.use(r => {
          ().pure[F]
        }).attempt.void
      catch
        case e: RuntimeException => ().pure

    r.map { _ =>
      // resource is correctly closed
      assert(c1.initCount == 1)
      assert(c1.closeCount == 1, "c1 close")
      assert(c2.initCount == 0)
      assert(c2.closeCount == 0)
      assert(closeOrder == Vector("c1"))
      ()
    }
  }
  }

  private class TestCloseable(val id: String, closeOrder: mutable.Buffer[String]):
    var initCount = 0
    var closeCount = 0
    @volatile var currentThread: Thread = null

    def init(throwOnClose: Boolean = false): AutoCloseable =
      assertSameThread()
      initCount += 1
      () => {
        assertSameThread()
        closeCount += 1
        closeOrder += id
        if (throwOnClose) throw new RuntimeException()
      }

    private def assertSameThread() = currentThread match
      case null => currentThread = Thread.currentThread()
      case t =>
        if ! (Thread.currentThread() eq t) then println("not current thread on close(")
        assert(Thread.currentThread() eq t)

