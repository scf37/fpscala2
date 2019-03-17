package me.scf37.fpscala2.module

import cats.Monad
import cats.effect.Effect
import cats.implicits._
import com.twitter.finagle.Http
import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.util.Await
import com.twitter.util.Future
import me.scf37.fpscala2.module.config.ServerConfig

trait ServerModule[I[_], F[_]] {
  def server: I[() => Unit]
}

case class ServerModuleImpl[I[_]: Later: Monad, F[_]: Effect](
  webModule: WebModule[I, F],
  commonModule: CommonModule[I, F],
  config: ServerConfig
) extends ServerModule[I, F] {

  override lazy val server: I[() =>  Unit] = for {
    service <- webModule.service
    log <- commonModule.log
  } yield () => {
    val f0 = { (req: Request) =>
      val io = service(req)

      Future value Effect[F].toIO(io).unsafeRunSync
    }
    val addr = config.interface + ":" + config.port
    val server = Http.serve(addr, Service.mk(f0))
    log.logInfo(s"HTTP Server is listening on $addr")
    Await.ready(server)

  }
}