package me.scf37.fpscala2.acceptance

import cats.Parallel
import cats.Monad
import cats.effect.IO
import cats.effect.Async
import cats.effect.Sync
import cats.data.Kleisli
import com.twitter.finagle.http.{Request, RequestBuilder, Response}
import com.twitter.io.Buf
import me.scf37.fpscala2.int.IntegrationApp
import me.scf37.fpscala2.module.init.{Indi, IndiContext}
import me.scf37.fpscala2.psql.EmbeddedPostgres
import org.scalatest.freespec.AnyFreeSpec
import cats.effect.unsafe.implicits.global
import me.scf37.fpscala2.service.JsonService
import cats.implicits._
import me.scf37.fpscala2.Application
import me.scf37.fpscala2.util.TestApplication
import org.scalatest.BeforeAndAfterAll

import java.sql.Connection

class TodoAcceptanceTest
  extends TodoAcceptanceTestF[IO]([A] => (io: IO[A]) => io.unsafeRunSync())
    with TestApplication[IO, Kleisli[IO, Connection, *]](IntegrationApp.Acceptance.app, [A] => (io: IO[A]) => io.unsafeRunSync())

trait TodoAcceptanceTestF[F[_]: Async: Parallel](eval: [A] => F[A] => A)
  extends AnyFreeSpec with TestApplication[F, Kleisli[F, Connection, *]]:

  lazy val service = app.webModule.service
  lazy val om = app.commonModule.json
  lazy val rnd = Sync[F].delay(System.nanoTime().toString)

  "list" - {
    "does not fail" in tf {
      for
        r <- get("/items")
        _ = assert(r.statusCode == 200)
        _ = assert(r.contentString.nonEmpty)
      yield ()
    }

    "contains newly created item" in tf {
      for
        id <- rnd
        text <- rnd.map(rnd => s"test-$rnd")
        posted <- post(s"/items/$id", Map("text" -> text))
        _ = assert(posted.statusCode == 200)
        r <- get("/items")
        _ = assert(r.contentString.contains(id))
        _ = assert(r.contentString.contains(text))
      yield ()
    }
  }

  "get" - {
    "returns existing item" in tf {
      for
        id <- rnd
        text <- rnd.map(rnd => s"test-$rnd")
        posted <- post(s"/items/$id", Map("text" -> text))
        _ = assert(posted.statusCode == 200)
        r <- get(s"/items/$id")
        _ = assert(r.contentString == s"""{"id":"$id","text":"$text"}""")
      yield ()
    }

    "fails on non-existing item" in tf {
      for
        id <- rnd
        r <- get(s"/items/$id")
        _ = assert(r.statusCode == 404)
      yield ()
    }
  }

  "post" - {
    "creates new item" in tf {
      for
        id <- rnd
        text <- rnd.map(rnd => s"test-$rnd")
        posted <- post(s"/items/$id", Map("text" -> text))
        _ = assert(posted.statusCode == 200)
        r <- get(s"/items/$id")
        _ = assert(r.contentString == s"""{"id":"$id","text":"$text"}""")
      yield ()
    }

    "fails if item already exists" in tf {
      for
        id <- rnd
        text <- rnd.map(rnd => s"test-$rnd")
        posted <- post(s"/items/$id", Map("text" -> text))
        _ = assert(posted.statusCode == 200)
        posted2 <- post(s"/items/$id", Map("text" -> text))
        _ = assert(posted2.statusCode == 400)
      yield ()
    }

    "validation is present" in tf {
      for
        r <- post(s"/items/invalid-id", Map("text" -> Seq.fill(101)('A').mkString))
        _ = assert(r.statusCode == 400)
        _ = assert(r.contentString ==
          """{"errors":["id: must only contain alphanumeric characters or underscore","text: length is not less than or equal 100"]}""")
      yield ()
    }
  }

  "put" - {
    "updates existing item" in tf {
      for
        id <- rnd
        text <- rnd.map(rnd => s"test-$rnd")
        text2 <- rnd.map(rnd => s"test-$rnd")
        posted <- post(s"/items/$id", Map("text" -> text))
        _ = assert(posted.statusCode == 200)
        putResult <- put(s"/items/$id", Map("text" -> text2))
        _ = assert(putResult.statusCode == 200)
        r <- get(s"/items/$id")
        _ = assert(r.contentString == s"""{"id":"$id","text":"$text2"}""")
      yield ()
    }

    "fails to update missing item" in tf {
      for
        id <- rnd
        putResult <- put(s"/items/$id", Map("text" -> "text"))
        _ = assert(putResult.statusCode == 404)
      yield()
    }
  }

  "delete" - {
    "deletes existing item" in tf {
      for
        id <- rnd
        text <- rnd.map(rnd => s"test-$rnd")
        posted <- post(s"/items/$id", Map("text" -> text))
        _ = assert(posted.statusCode == 200)
        deleted <- delete(s"/items/$id")
        _ = assert(deleted.statusCode == 200)
      yield ()
    }

    "does not fail on missing item" in tf {
      for
        id <- rnd
        deleted <- delete(s"/items/$id")
        _ = assert(deleted.statusCode == 200)
      yield ()
    }
  }

  private def tf(f: => F[Unit]): Unit =
    val test =
      for
        service <- service
        json <- om
        _ <- f
      yield()
    eval(test)

  private def get(path: String): F[Response] =
    for
      service <- service
      response <- service(RequestBuilder().url(s"http://local/api/v1$path").buildGet())
    yield response

  private def post(path: String, content: Map[String, String]): F[Response] =
    for
      service <- service
      req <- json(content)
      response <- service(RequestBuilder().url(s"http://local/api/v1$path").buildPost(req))
    yield response

  private def put(path: String, content: Map[String, String]): F[Response] =
    for
      service <- service
      req <- json(content)
      response <- service(RequestBuilder().url(s"http://local/api/v1$path").buildPut(req))
    yield response

  private def delete(path: String): F[Response] =
    for
      service <- service
      response <- service(RequestBuilder().url(s"http://local/api/v1$path").buildDelete())
    yield response

  private def json(content: Map[String, String]): F[Buf] =
    for
      om <- om
      buf <- om.write(content)
    yield Buf.Utf8(buf)
