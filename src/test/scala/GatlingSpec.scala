import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps


class GatlingSpec extends Simulation {

  val N_users = 100
  val T_seconds = 5
  val K_requests = 10000

  var counter = 0
  def ffeeder(n: Int) = Iterator.continually(Map("token" -> {counter += 1
    counter % n}))

  val host = Option(System.getProperty("host")).getOrElse("localhost")
  val httpConf = http.baseURL(s"http://localhost:8080")
  val scn = scenario("rrrrr")
      .feed(ffeeder(N_users))
      .exec(http("loadtest")
        .get("/")
        .header("Authorization","${token}")
        .check(status.is(200)))

  setUp(
    scn.inject(rampUsers(K_requests) over(T_seconds seconds)).protocols(httpConf)
  )
}