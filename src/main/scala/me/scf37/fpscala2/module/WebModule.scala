package me.scf37.fpscala2.module

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import com.twitter.finagle.http.Method
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import me.scf37.fpscala2.controller.TodoRequest
import me.scf37.fpscala2.http.ExceptionFilter
import me.scf37.fpscala2.http.Route
import me.scf37.fpscala2.module.init.Init
import tethys.JsonReader
import tethys.JsonWriter


case class WebModule[I[_], F[_]](
  service: I[Request => F[Response]]
):
  def mapK[II[_]](f: [A] => I[A] => II[A]): WebModule[II, F] = WebModule(
    service = f(service)
  )


object WebModule:

  def apply[I[_]: Monad, F[_]: Sync](
    controllerModule: ControllerModule[I, F],
    commonModule: CommonModule[I, F]
  )(
    using init: Init[I, F]
  ): WebModule[I, F] =
    val route: I[Route[F, Response]] = (controllerModule.todoController, commonModule.json).mapN {
      case (todoController, json) =>
        def toJson[T: JsonWriter](v: F[T]): F[Response] = for
          v <- v
          jsonStr <- json.write(v)
        yield
          val r = Response(Status.Ok)
          r.setContentTypeJson()
          r.contentString = jsonStr
          r


        def fromJson[T: JsonReader](req: Request): F[T] =
          json.read[T](req.contentString)

        List(
          Route.mk[F, Response](Method.Get, "/api/v1/items")((req, ctx) => toJson(todoController.list())),

          Route.mk[F, Response](Method.Get, "/api/v1/items/:id")((req, ctx) =>
            toJson(todoController.get(ctx(":id")))),

          Route.mk[F, Response](Method.Post, "/api/v1/items/:id")((req, ctx) =>
            fromJson[TodoRequest](req).flatMap { r =>
              toJson(todoController.create(ctx(":id"), r.validate(ctx(":id"))))
            }
          ),

          Route.mk[F, Response](Method.Put, "/api/v1/items/:id")((req, ctx) =>
            fromJson[TodoRequest](req).flatMap { r =>
              toJson(todoController.update(ctx(":id"), r.validate(ctx(":id"))))
            }
          ),

          Route.mk[F, Response](Method.Delete, "/api/v1/items/:id")((req, ctx) =>
            toJson(todoController.delete(ctx(":id")))
          )
        ).combineAll
    }

    val service: I[Request => F[Response]] = (route, commonModule.json, commonModule.log).mapN {
      case (route, om, log) =>
        val filters = new ExceptionFilter[F](om, log)
        filters(req => route(req).getOrElse(Response.apply(Status.NotFound).pure[F]))
    }

    WebModule(service = service)
