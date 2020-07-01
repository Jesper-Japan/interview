package forex.services.rates

object errors {

  sealed trait Error
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
    final case class OneFrameCurrencyNotSupported() extends Error
    final case class OneFrameQuotaReached() extends Error
  }


}
