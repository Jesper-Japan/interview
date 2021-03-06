package forex.services

import java.time.OffsetDateTime

import cats.Applicative
import forex.config._
import forex.domain.{Currency, Rate}
import forex.services.rates.interpreters.OneFrameInterpreter
import org.junit.jupiter.api.Test

class OneFramePerformanceTest {

  // TODO: Refactor into separate config file
  def initConfig(): ExchangeService = {
    new ExchangeService(
      protocol = "http",
      host = "192.168.99.101",
      port = 8080,
      service = "rates",
      param = "pair",
      token = "10dc303535874aeccc86a8251e6992f5"
    )
  }

  def testOneFrameInterpreterInit[F[_]: Applicative](): OneFrameInterpreter[F] = {
    new OneFrameInterpreter[F](initConfig())
  }

  @Test
  // Tests that there's throughput and that the pair sent is the same one that's returned
  def onePairThroughputTest() = {
    val oneFrameInterpreter = testOneFrameInterpreterInit();
    val requestPair = Rate.Pair(from = Currency.fromString("USD"), to = Currency.fromString("JPY"))
    val result = oneFrameInterpreter.get(requestPair)

    assert(result.isRight)

    val resultPair = result.right.get

    assert(resultPair.pair.to == requestPair.to)
    assert(resultPair.pair.from == requestPair.from)
  }

  @Test
  // Tests that the response is less than 5 minutes old
  def onePairTimelinessTest() = {
    val oneFrameInterpreter = testOneFrameInterpreterInit();
    val requestPair = Rate.Pair(from = Currency.fromString("USD"), to = Currency.fromString("EUR"))
    val nowMinus5 = OffsetDateTime.now.minusMinutes(5)
    val result = oneFrameInterpreter.get(requestPair)

    assert(result.isRight)

    val resultRate = result.right.get
    val returnTime = resultRate.timestamp.value
    val returnTimeLocalTimeZone = returnTime.atZoneSameInstant(OffsetDateTime.now.getOffset)
    val compareResult = returnTimeLocalTimeZone.compareTo(nowMinus5.toZonedDateTime)
    assert(compareResult > 0)
  }

  @Test
  // Tests that the response is less than 5 minutes old 100 times
  def multipleTimelinessTest() = {
    val oneFrameInterpreter = testOneFrameInterpreterInit();
    val requestPair = Rate.Pair(from = Currency.fromString("USD"), to = Currency.fromString("EUR"))
    var fastCount = 0
    for (i <- 1 to 100) {
      val nowMinus5 = OffsetDateTime.now.minusMinutes(5)
      val result = oneFrameInterpreter.get(requestPair)
      val resultRate = result.right.get
      val returnTime = resultRate.timestamp.value
      val returnTimeLocalTimeZone = returnTime.atZoneSameInstant(OffsetDateTime.now.getOffset)
      val compareResult = returnTimeLocalTimeZone.compareTo(nowMinus5.toZonedDateTime)
      if(compareResult > 0) fastCount+=1
    }
    assert(fastCount == 100)
  }

  @Test
  // Tests that the response is less than 5 minutes old 100 times
  def thousandRepetitionsTest() = {
    val oneFrameInterpreter = testOneFrameInterpreterInit();
    val requestPair = Rate.Pair(from = Currency.fromString("USD"), to = Currency.fromString("EUR"))
    var count = 0
    for (i <- 1 to 1000) {
      val result = oneFrameInterpreter.get(requestPair)
      if(result.isRight) count+=1
    }
    assert(count == 1000)
  }

  @Test
  // Tests that the response is less than 5 minutes old 100 times
  def tenThousandRepetitionsTest() = {
    val oneFrameInterpreter = testOneFrameInterpreterInit();
    val requestPair = Rate.Pair(from = Currency.fromString("USD"), to = Currency.fromString("EUR"))
    var count = 0
    for (i <- 1 to 10000) {
      val result = oneFrameInterpreter.get(requestPair)
      if(result.isRight) count+=1
      if(count%100 == 0)Console.println(count)
    }
    assert(count == 10000)
  }
}
