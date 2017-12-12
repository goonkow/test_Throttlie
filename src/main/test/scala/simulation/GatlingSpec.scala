package simulation

import java.io.{File, FileInputStream, InputStream}

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

class GatlingSpec extends Simulation {

  val host = Option(System.getProperty("host")).getOrElse("localhost")
  val httpConf = http.baseURL(s"http://$host:8080/")
  val scenario = scenario("test")
    .exec(Index.request)


  setUp(
    scenario.inject(constantUsersPerSec(100) during(20 seconds)).protocols(httpConf)
  )
}

object Index {
  def request = http("loadtest").get("").header("Authorization","token")
    .check(status.is(200))

}
