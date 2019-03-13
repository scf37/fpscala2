package me.scf37.fpscala2.config.module

import cats.Monad
import cats.effect.IO
import com.twitter.finagle.Http
import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.util.Await
import com.twitter.util.Future
import me.scf37.fpscala2.config.Later
import cats.implicits._
import me.scf37.fpscala2.config.ServerConfig

trait ServerModule[F[_], I[_]] {
  def server: I[() => Unit]
}

case class ServerModuleImpl[I[_]: Later: Monad](
  webModule: WebModule[IO, I],
  config: ServerConfig
) extends ServerModule[IO, I] {

  override lazy val server: I[() =>  Unit] = for {
    service <- webModule.service
  } yield () => {
    val f0 = { (req: Request) =>
      val io = service(req)

      Future value io.unsafeRunSync
    }

    val server = Http.serve(config.interface + ":" + config.port, Service.mk(f0))

    Await.ready(server)

  }
}