package forex.services.rates

import cats.Applicative
import forex.config.ExchangeService
import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative](): Algebra[F] = new OneFrameDummy[F]()
  def oneFrameInterpreter[F[_]: Applicative](exchangeService: ExchangeService): Algebra[F] = new OneFrameInterpreter[F](exchangeService)
}
