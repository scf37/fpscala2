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

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

case class ServerModule[I[_], F[_]](
  server: I[() => Unit]
)

object ServerModule {

  def apply[I[_]: Later: Monad, F[_]: Async : Dispatcher](
    webModule: WebModule[I, F],
    commonModule: CommonModule[I, F],
    config: ServerConfig
  ): ServerModule[I, F] = {

    val server = for {
      service <- webModule.service
      log <- commonModule.log
    } yield () => {
      val f0 = { (req: Request) =>
        val io = service(req)

        val ff = Promise[Response]
        val (future, cancelToken) = summon[Dispatcher[F]].unsafeToFutureCancelable(io)
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
      val server = Http.serve(addr, Service.mk(f0))
      log.logInfo(s"HTTP Server is listening on $addr")
      Await.ready(server)
      ()
    }

    ServerModule[I, F](
      server = server
    )
  }

}
