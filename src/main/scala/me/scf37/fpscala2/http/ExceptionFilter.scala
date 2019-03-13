package me.scf37.fpscala2.http

import cats.effect.Sync
import cats.implicits._
import com.fasterxml.jackson.core.JsonProcessingException
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassMappingException
import me.scf37.fpscala2.exception.RestException

/**
  * Convert error to REST error response
  *
  */
class ExceptionFilter[F[_]: Sync](om: FinatraObjectMapper) extends Filter[F] {
  override def apply(orig: Request => F[Response]): Request => F[Response] = req => {

    Sync[F].defer(orig(req)).recoverWith {
      case e: RestException => respond(Status.BadRequest, Seq(e.getMessage))

      case e: CaseClassMappingException => respond(Status.BadRequest, e.errors.map(_.getMessage()))

      case e: JsonProcessingException =>
        val loc = Option(e.getLocation).map { loc =>
          s" [line: ${loc.getLineNr}, column: ${loc.getColumnNr}]"
        }
        e.clearLocation()
        respond(Status.BadRequest, Seq(e.getMessage + loc))


      case e: Throwable =>
        e.printStackTrace()
        respond(Status.InternalServerError, Seq("Internal Server Error"))
    }
  }

  private def respond(status: Status, errors: Seq[String]): F[Response] = {
    val r = Response(status)
    r.setContentTypeJson()
    r.content(om.writeValueAsBuf(Map("errors" -> errors)))
    r.pure[F]
  }
}
