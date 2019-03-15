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

trait ServerModule[F[_], I[_]] {
  def server: I[() => Unit]
}

case class ServerModuleImpl[F[_]: Effect, I[_]: Later: Monad](
  webModule: WebModule[F, I],
  commonModule: CommonModule[F, I],
  config: ServerConfig
) extends ServerModule[F, I] {

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