package forex.services.rates.interpreters

import java.time.OffsetDateTime

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.config.ExchangeService
import forex.domain.{Price, Rate, Timestamp}
import forex.services.rates.Algebra
import forex.services.rates.errors._
import play.api.libs.json.{Json, Reads}
import scalaj.http.Http

class OneFrameInterpreter[F[_]: Applicative] (exchangeService: ExchangeService) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    val url = exchangeService.protocol + "://" + exchangeService.host + ":" + exchangeService.port + "/" + exchangeService.service
    val result = Http(url).headers(Seq("token" -> exchangeService.token)).param(exchangeService.param, pair.from.toString() + pair.to.toString()).asString
    val parsedJson = Json.parse(result.body)
    val deserialized = parsedJson.as[Seq[ExchangeResponseObject]]
    val responseObject = deserialized.head
    val timestamp = Timestamp(OffsetDateTime.parse(responseObject.time_stamp))
    Rate(pair, Price(responseObject.price), timestamp).asRight[Error].pure[F]
  }

  case class ExchangeResponseObject(
     from: String,
     to: String,
     bid: BigDecimal,
     ask: BigDecimal,
     price: BigDecimal,
     time_stamp: String
   )

    implicit val ExchangeResponseObjectReads: Reads[ExchangeResponseObject] = Json.reads[ExchangeResponseObject]

}
