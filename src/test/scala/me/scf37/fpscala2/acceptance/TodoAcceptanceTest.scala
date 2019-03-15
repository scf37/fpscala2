package me.scf37.fpscala2.acceptance

import cats.Eval
import cats.effect.IO
import com.twitter.finagle.http.RequestBuilder
import me.scf37.fpscala2.int.IntegrationApp
import me.scf37.fpscala2.psql.EmbeddedPostgres
import org.scalatest.FreeSpec

class TodoAcceptanceTest extends FreeSpec {
  import me.scf37.fpscala2.db.sql._
  val app = IntegrationApp.make[Eval, IO, SqlDb[IO, ?]](EmbeddedPostgres.acceptanceInstance)
  val service = app.webModule.service.value
  val om = app.commonModule.json.value

  def d = System.nanoTime().toString

  "list" - {
    "does not fail" in {
      val r = get("/items")
      assert(r.statusCode == 200)
      assert(r.contentString.nonEmpty)
    }

    "contains newly created item" in {
      val id = d
      val text = s"test-$d"
      assert(post(s"/items/$id", Map("text" -> text)).statusCode == 200)
      val r = get("/items")
      assert(r.contentString.contains(id))
      assert(r.contentString.contains(text))
    }
  }

  "get" - {
    "returns existing item" in {
      val id = d
      val text = s"test-$d"
      assert(post(s"/items/$id", Map("text" -> text)).statusCode == 200)
      assert(get(s"/items/$id").contentString == s"""{"id":"$id","text":"$text"}""")
    }

    "fails on non-existing item" in {
      assert(get(s"/items/$d").statusCode == 404)
    }
  }

  "post" - {
    "creates new item" in {
      val id = d
      val text = s"test-$d"
      assert(post(s"/items/$id", Map("text" -> text)).statusCode == 200)
      assert(get(s"/items/$id").contentString == s"""{"id":"$id","text":"$text"}""")
    }

    "fails if item already exists" in {
      val id = d
      val text = s"test-$d"
      assert(post(s"/items/$id", Map("text" -> text)).statusCode == 200)
      assert(post(s"/items/$id", Map("text" -> text)).statusCode == 400)
    }
  }

  "put" - {
    "updates existing item" in {
      val id = d
      val text = s"test-$d"
      val text2 = s"test-$d"
      assert(post(s"/items/$id", Map("text" -> text)).statusCode == 200)
      assert(put(s"/items/$id", Map("text" -> text2)).statusCode == 200)
      assert(get(s"/items/$id").contentString == s"""{"id":"$id","text":"$text2"}""")
    }

    "fails to update missing item" in {
      assert(put(s"/items/$d", Map("text" -> "text")).statusCode == 404)
    }
  }

  "delete" - {
    "deletes existing item" in {
      val id = d
      val text = s"test-$d"
      assert(post(s"/items/$id", Map("text" -> text)).statusCode == 200)
      assert(delete(s"/items/$id").statusCode == 200)
    }

    "does not fail on missing item" in {
      assert(delete(s"/items/$d").statusCode == 200)
    }
  }

  private def get(path: String) =
    service(RequestBuilder().url(s"http://local$path").buildGet()).unsafeRunSync()

  private def post(path: String, content: Map[String, Any]) =
    service(RequestBuilder().url(s"http://local$path").buildPost(om.writeValueAsBuf(content))).unsafeRunSync()

  private def put(path: String, content: Map[String, Any]) =
    service(RequestBuilder().url(s"http://local$path").buildPut(om.writeValueAsBuf(content))).unsafeRunSync()

  private def delete(path: String) =
    service(RequestBuilder().url(s"http://local$path").buildDelete()).unsafeRunSync()
}
