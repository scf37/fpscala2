package me.scf37.fpscala2.module

import cats.Monad
import cats.effect.Async
import cats.effect.Sync
import cats.effect.std.Dispatcher
import cats.implicits._
import com.twitter.finagle.Http
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.util.Promise
import me.scf37.fpscala2.module.config.ServerConfig
import me.scf37.fpscala2.module.init.Init

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

case class ServerModule[I[_], F[_]](
  server: I[() => Unit]
):
  def mapK[II[_]](f: [A] => I[A] => II[A]): ServerModule[II, F] = ServerModule(
    server = f(server)
  )


object ServerModule:

  def apply[I[_]: Monad, F[_]: Async](
    webModule: WebModule[I, F],
    commonModule: CommonModule[I, F],
    config: I[ServerConfig]
  )(using init: Init[I, F]): ServerModule[I, F] =
    val serverFun =
      for
        config <- config
        dispatcher <- init.liftR(Dispatcher[F])
        server <- (webModule.service, commonModule.log).mapN { case (service, log) =>
          val f0 = { (req: Request) =>
            val io = service(req)

            val ff = Promise[Response]
            val (future, cancelToken) = dispatcher.unsafeToFutureCancelable(io)
            ff.setInterruptHandler {
              case _ => cancelToken()
            }
            future.onComplete {
              case Failure(ex) => ff.setException(ex)
              case Success(value) => ff.setValue(value)
            }(using ExecutionContext.global)

            ff
          }
          val addr = config.interface + ":" + config.port
          for
            _ <- init liftF log.logInfo(s"HTTP Server is listening on $addr")
            server <- init delay Http.serve(addr, Service.mk(f0))
          yield server
        }.flatten
      yield () => {
        Await.ready(server)
        ()
      }

    ServerModule[I, F](
      server = serverFun
    )
