package test.throttly

import akka.actor._
import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorSystem, Inbox, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpHeader, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._

object Main extends App {

  implicit val system = ActorSystem("http-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  implicit val timeout = Timeout(1 second)

  val slaService = new SlaServiceImplTest(5)
  val throttler = system.actorOf(Props(new ThrottlingServiceImpl(1, slaService)))
  val inbox = Inbox.create(system)

  scala.sys.addShutdownHook {
    system.terminate()
    Await.result(system.whenTerminated, 5.seconds)
  }

  val routes = {
    path("") {
      extractRequest { request =>
        val token: Option[String] =
        try {
          Some(request.headers.filter(h => h.name == "Authorization").head.value)
        } catch  {
          case e: Exception => None
        }

        val future = throttler ? IsRequestAllowed(token)
        val result = Await.result(future, 5.seconds)

        complete(HttpResponse(entity=result.toString))
      }
    } ~
    path("without") {
      get {
        complete("It's ALIVE!")
      }
    }
  }

  Http().bindAndHandle(routes, "0.0.0.0", 8080).map { binding =>
  } recoverWith { case _ => system.terminate() }

}
