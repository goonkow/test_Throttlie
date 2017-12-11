package test.throttly


import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.concurrent.Await
import scala.concurrent.duration._


object Main extends App {

  implicit val system = ActorSystem("http-system")
  implicit val materializer = ActorMaterializer()
  implicit private val executionContext = system.dispatcher

  scala.sys.addShutdownHook {
    system.terminate()
    Await.result(system.whenTerminated, 5.seconds)
  }

  val routes = {
    get {
      path("test") {
        complete("It's ALIVE!")
      }
    }
  }

}
