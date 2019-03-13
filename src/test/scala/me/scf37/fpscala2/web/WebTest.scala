package me.scf37.fpscala2.web

import cats.Eval
import com.twitter.finagle.http.RequestBuilder
import com.twitter.io.Buf
import me.scf37.fpscala2.memory.MemoryApp
import org.scalatest.FreeSpec

class WebTest extends FreeSpec {
  val app = new MemoryApp[Eval]
  val service = app.webModule.service.value

  "404 is empty response" in {
    val r = service(url("/404").buildGet()).unsafeRunSync()
    assert(r.statusCode == 404)
    assert(r.contentString.length == 0)
  }

  "empty request results in error message" in {
    val r = service(url("/items/142").buildPut(Buf.Empty)).unsafeRunSync()
    assert(r.statusCode == 400)
    assert(r.contentString == """{"errors":["No content to map due to end-of-input [line: 1, column: 0]"]}""")
  }

  "invalid json results in error message" in {
    val r = service(url("/items/142").buildPut(Buf.Utf8("{\"a\": q}"))).unsafeRunSync()
    assert(r.statusCode == 400)
    assert(r.contentString == """{"errors":["Unrecognized token 'q': was expecting ('true', 'false' or 'null') [line: 1, column: 9]"]}""")
  }

  "empty json results in validation message" in {
    val r = service(url("/items/142").buildPut(Buf.Utf8("{}"))).unsafeRunSync()
    assert(r.statusCode == 400)
    assert(r.contentString == """{"errors":["id: field is required","text: field is required"]}""")
  }

  "unknown fields in request are ignored" in {
    val r = service(url("/items/142").buildPost(
      Buf.Utf8("""{"oops": 42, "id":"t1", "text": "text"}"""))
    ).unsafeRunSync()
    assert(r.statusCode == 200)
    assert(r.contentString == """{"id":"142","text":"text"}""")
  }

  private def url(path: String) = RequestBuilder().url(s"http://local$path")

}
