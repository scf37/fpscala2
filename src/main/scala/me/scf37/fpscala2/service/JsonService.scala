package me.scf37.fpscala2.service

import cats.effect.Sync
import me.scf37.fpscala2.exception.ValidationFailedException
import tethys._
import tethys.jackson._
import cats.implicits._
import com.fasterxml.jackson.core.JsonParseException

/**
  * JSON serializer/deserializer, together with proper error formatting
  *
  * @tparam F
  */
trait JsonService[F[_]]:

  def read[T: JsonReader](json: String): F[T]

  def write[T: JsonWriter](value: T): F[String]


object JsonService:
  def apply[F[_]: Sync]: JsonService[F] = new JsonService[F]:

    override def read[T: JsonReader](json: String): F[T] =
      json.jsonAs[T] match

        case Left(error) if error.getCause.isInstanceOf[JsonParseException] =>
          val e = error.getCause.asInstanceOf[JsonParseException]
          val loc = Option(e.getLocation).map { loc =>
            s" [line: ${loc.getLineNr}, column: ${loc.getColumnNr}]"
          }
          e.clearLocation()
          val msg = e.getMessage + loc.getOrElse("")
          Sync[F].raiseError(ValidationFailedException(Seq(msg)))

        case Left(error) => Sync[F].raiseError(ValidationFailedException(Seq(error.getMessage)))

        case Right(value) => value.pure[F]

    override def write[T: JsonWriter](value: T): F[String] =
      value.asJson.pure[F]
