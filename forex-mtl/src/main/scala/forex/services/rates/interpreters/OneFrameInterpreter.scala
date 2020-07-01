package forex.services.rates.interpreters

import java.net.UnknownHostException
import java.time.OffsetDateTime

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.config.ExchangeService
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.Algebra
import forex.services.rates.errors.Error._
import forex.services.rates.errors._
import play.api.libs.json.{Json, Reads}
import scalaj.http.Http

class OneFrameInterpreter[F[_]: Applicative] (exchangeService: ExchangeService) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    if(pair.from == Currency.UNKNOWN || pair.to == Currency.UNKNOWN) {
      val errorAsArray = Array[Error](OneFrameCurrencyNotSupported()) // The compiler wouldn't infer that OneFrameCurrencyNotSupported extends Error, so I had to pass it as an object in an array to get it to compile.
      return errorAsArray.head.asLeft[Rate].pure[F]
    }
    try {
      val url = exchangeService.protocol + "://" + exchangeService.host + ":" + exchangeService.port + "/" + exchangeService.service
      val result = Http(url).headers(Seq("token" -> exchangeService.token)).param(exchangeService.param, pair.from.toString() + pair.to.toString()).asString
      if(result.body contains("Quota reached")) {
        val errorAsArray = Array[Error](OneFrameQuotaReached()) // The compiler wouldn't infer that OneFrameCurrencyNotSupported extends Error, so I had to pass it as an object in an array to get it to compile.
        return errorAsArray.head.asLeft[Rate].pure[F]
      }
      val parsedJson = Json.parse(result.body)
      val deserialized = parsedJson.as[Seq[ExchangeResponseObject]]
      val responseObject = deserialized.head
      val timestamp = Timestamp(OffsetDateTime.parse(responseObject.time_stamp))
      Rate(Rate.Pair(from = Currency.fromString(responseObject.from), to = Currency.fromString(responseObject.to)), Price(responseObject.price), timestamp).asRight[Error].pure[F]
    } catch {
      case _ : UnknownHostException => {
        val errorAsArray = Array[Error](OneFrameLookupFailed("We couldn't locate the OneFrame service")) // The compiler wouldn't infer that OneFrameCurrencyNotSupported extends Error, so I had to pass it as an object in an array to get it to compile.
        errorAsArray.head.asLeft[Rate].pure[F]
      }
    }
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
