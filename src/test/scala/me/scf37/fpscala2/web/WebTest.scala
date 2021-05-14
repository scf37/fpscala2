package me.scf37.fpscala2.web

import cats.Parallel
import cats.effect.{Async, IO}
import cats.effect.std.Dispatcher
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.twitter.finagle.http.{Request, RequestBuilder, Response}
import com.twitter.io.Buf
import me.scf37.fpscala2.Application
import me.scf37.fpscala2.int.IntegrationApp
import me.scf37.fpscala2.module.init.{Indi, IndiContext}
import me.scf37.fpscala2.util.TestApplication
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import IntegrationApp.Memory.{Eff, TestState}
import me.scf37.fpscala2.int.IntegrationApp.Memory
import me.scf37.fpscala2.model.Todo
import me.scf37.fpscala2.typeclass.Ask

class WebTest extends WebTestF[Eff]([A] => (io: Eff[A]) => io.run(Memory.initialState).unsafeRunSync())
  with TestApplication[Eff, Eff](IntegrationApp.Memory.app, [A] => (io: Eff[A]) => io.run(Memory.initialState).unsafeRunSync())

trait WebTestF[F[_]: Async: Parallel](eval: [A] => F[A] => A)(using testState: Ask[F, TestState]) extends AnyFreeSpec with TestApplication[F, F]:
  lazy val service = app.webModule.service

  "404 is empty response" in fp { service =>
    for
      r <- service(url("/404").buildGet())
      _ = assert(r.statusCode == 404)
      _ = assert(r.contentString.length == 0)
    yield ()
  }

  "empty request results in error message" in fp { service =>
    for
      r <- service(url("/items/142").buildPut(Buf.Empty))
      _ = assert(r.statusCode == 400)
      _ = assert(r.contentString == """{"errors":["Illegal json at '[ROOT]': Expected object start but found: Empty"]}""")
    yield ()
  }

  "invalid json results in error message" in fp { service =>
    for
      r <- service(url("/items/142").buildPut(Buf.Utf8("{\"a\": q}")))
      _ = assert(r.statusCode == 400)
      _ = assert(r.contentString == """{"errors":["Unrecognized token 'q': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false') [line: 1, column: 8]"]}""")
    yield ()
  }

  "empty json results in validation message" in fp { service =>
    for
      r <- service(url("/items/142").buildPut(Buf.Utf8("{}")))
      _ = assert(r.statusCode == 400)
      // see https://github.com/tethys-json/tethys/issues/51
      _ = assert(r.contentString == """{"errors":["Illegal json at '[ROOT]': Can not extract fields 'text'"]}""")
    yield ()
  }

  "unknown fields in request are errors" in fp { service =>
    for
      r <- service(url("/items/142").buildPost(
        Buf.Utf8("""{"oops": 42, "id":"t1", "text": "text"}"""))
      )
      _ = assert(r.statusCode == 400)
      _ = assert(r.contentString == """{"errors":["Illegal json at '[ROOT]': unexpected field 'oops', expected one of 'text'"]}""")
    yield ()
  }

  "nulls are not accepted for required fields" in fp { service =>
    for
      r <- service(url("/items/241").buildPost(
        Buf.Utf8("""{"id":"t1", "text": null}"""))
      )
      _ = assert(r.statusCode == 400)
      _ = assert(r.contentString == """{"errors":["Illegal json at '[ROOT]': unexpected field 'id', expected one of 'text'"]}""")
    yield ()
  }

  "saved todos appear in the database" in fp { service =>
    for
      r <- service(url("/items/241").buildPost(
        Buf.Utf8("""{"text": "hello"}"""))
      )
      dbState <- testState.ask.map(_.daoState)
      _ = assert(dbState.todos == Vector(Todo(id = "241", text = "hello")))
      _ = assert(r.statusCode == 200)
    yield ()
  }

  private def url(path: String) = RequestBuilder().url(s"http://local/api/v1$path")

  private def fp(f: (Request => F[Response]) => F[Unit]): Unit = eval(service.flatMap(f))
