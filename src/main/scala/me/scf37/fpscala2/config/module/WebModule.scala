package me.scf37.fpscala2.config.module

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import com.twitter.finagle.http.Method
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import me.scf37.fpscala2.config.Later
import me.scf37.fpscala2.http.ExceptionFilter
import me.scf37.fpscala2.http.Route
import me.scf37.fpscala2.model.Todo


trait WebModule[F[_], I[_]] {
  def service: I[Request => F[Response]]
}

class WebModuleImpl[F[_]: Sync, I[_]: Monad: Later](
  controllerModule: ControllerModule[F, I],
  commonModule: CommonModule[F, I]
) extends WebModule[F, I] {

  private lazy val route: I[Route[F, Response]] = for {
    todoController <- controllerModule.todoController
    json <- commonModule.json
  } yield {

    def toJson[T](v: F[T]): F[Response] = v.map {v =>
      val r = Response(Status.Ok)
      r.setContentTypeJson()
      r.content = json.writeValueAsBuf(v)
      r
    }

    def fromJson[T: Manifest](req: Request): T = {
      json.parse[T](req.content)
    }


    List(
      Route.mk[F, Response](Method.Get, "/items")((req, ctx) => toJson(todoController.list())),

      Route.mk[F, Response](Method.Post, "/items/:id")((req, ctx) =>
        toJson(todoController.create(fromJson[Todo](req).copy(id = ctx(":id"))))
      ),

      Route.mk[F, Response](Method.Put, "/items/:id")((req, ctx) =>
        toJson(todoController.update(fromJson[Todo](req).copy(id = ctx(":id"))))
      ),

      Route.mk[F, Response](Method.Delete, "/items/:id")((req, ctx) =>
        toJson(todoController.delete(ctx(":id")))
      )
    ).combineAll
  }

  override lazy val service: I[Request => F[Response]] = for {
    route <- route
    om <- commonModule.json
  } yield {
    val filters = new ExceptionFilter[F](om)

    filters(req => route(req).getOrElse(Response.apply(Status.NotFound).pure[F]))
  }
}
