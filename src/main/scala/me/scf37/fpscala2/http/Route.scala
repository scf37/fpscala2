package me.scf37.fpscala2.http

import cats.kernel.Monoid
import com.twitter.finagle.http.Method
import com.twitter.finagle.http.Request

/**
  * HTTP Route is a function that tries to convert request to response.
  * If request does not match this route, it returns None
  *
  * @tparam F
  * @tparam Resp
  */
trait Route[F[_], Resp] extends (Request => Option[F[Resp]]):
  override def apply(req: Request): Option[F[Resp]]

object Route:
  def apply[F[_], Resp](f: Request => Option[F[Resp]]): Route[F, Resp] = req => f(req)

  def mk[F[_], Resp](method: Method, path: String)(f: (Request, Map[String, String]) => F[Resp]): Route[F, Resp] =
    Route { req =>
      matches(req, method, path).map { pathParams =>
        f(req, pathParams)
      }
    }

  private def matches(req: Request, method: Method, path: String): Option[Map[String, String]] =
    // intentionally imperative implementation
    // this method is called multiple times on every request so speed is more important than style
    def pathVariable(part: String): Boolean = part.startsWith(":")

    if req.method != method then return None

    val parts = path.split("/")
    val reqParts = req.path.split("/")

    if parts.length != reqParts.length then return None

    val pairs = parts.zip(reqParts)

    val sameParts = pairs.forall {
      case (part, _) if pathVariable(part) => true

      case (part, reqPart) => part == reqPart
    }

    if !sameParts then return None

    Some(pairs.filter(kv => pathVariable(kv._1)).toMap)


  given monoid[F[_], Resp]: Monoid[Route[F, Resp]] with
    override def empty: Route[F, Resp] = Route(_ => None)

    override def combine(x: Route[F, Resp], y: Route[F, Resp]): Route[F, Resp] = req => x(req).orElse(y(req))

