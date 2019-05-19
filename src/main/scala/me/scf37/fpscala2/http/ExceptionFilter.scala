package me.scf37.fpscala2.http

import cats.effect.Sync
import cats.implicits._
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import me.scf37.fpscala2.exception.RestException
import me.scf37.fpscala2.logging.Log
import me.scf37.fpscala2.service.JsonService

/**
  * Convert error to REST error response
  *
  */
class ExceptionFilter[F[_]: Sync](
  om: JsonService[F],
  log: Log[F]
) extends Filter[F] {

  override def apply(orig: Request => F[Response]): Request => F[Response] = req => {

    Sync[F].defer(orig(req)).recoverWith {

      case e: RestException => for {
        _ <- log.logValidationError(e.getMessage, e)
        r <- respond(Status.fromCode(e.status), e.errors)
      } yield r

      case e: Throwable =>
        for {
          _ <- log.logUnexpectedError("Unexpected error occured: " + e.toString, e)
          r <- respond(Status.InternalServerError, Seq("Internal Server Error"))
        } yield r
    }
  }

  private def respond(status: Status, errors: Seq[String]): F[Response] = {
    for {
      json <- om.write(ErrorResponse(errors.sorted))
    } yield {
      val r = Response(status)
      r.setContentTypeJson()
      r.contentString = json
      r
    }

  }
}
