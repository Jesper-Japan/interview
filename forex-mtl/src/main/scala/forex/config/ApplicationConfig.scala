package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    exchangeservice: ExchangeService
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class ExchangeService(
    protocol: String,
    host: String,
    port: Int,
    service: String,
    param: String,
    token: String
)
