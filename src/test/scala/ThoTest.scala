import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Inbox, Props}
import org.scalatest.{AsyncFunSuite, FunSuite}
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import test.throttly._

import scala.concurrent.Future
import scala.concurrent.duration._

class ThoTest extends AsyncFunSuite {

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val slaService = new SlaServiceImplTest(3)
  val inbox = Inbox.create(system)

  test("2req") {
    val throttler = system.actorOf(Props(new ThrottlingServiceImpl(2, slaService)))

    throttler ! IsRequestAllowed(None)
    (throttler ? IsRequestAllowed(None)) map {
      case x: Boolean => assert(x)
      case _ => fail("Wrong resp")
    }
  }

  test("3req") {
    val throttler = system.actorOf(Props(new ThrottlingServiceImpl(2, slaService)))

    throttler ! IsRequestAllowed(None)
    throttler ! IsRequestAllowed(None)
    (throttler ? IsRequestAllowed(None)) map {case x: Boolean => assert(!x)}
  }

  test("wait") {
    val throttler = system.actorOf(Props(new ThrottlingServiceImpl(2, slaService)))

    throttler ! IsRequestAllowed(Some("t"))
    throttler ! IsRequestAllowed(Some("t"))
    throttler ! IsRequestAllowed(Some("t"))
    Thread.sleep(1000)
    (throttler ? IsRequestAllowed(Some("t"))) map {case x: Boolean => assert(x)}
  }

  test("avoid duplicate requests") {
    var counter = 0
    val slaCountedService = new SlaService {
      override def getSlaByToken(token: String): Future[Sla] = {
        Future{
          counter += 1
          Thread.sleep(250)
          Sla(token, 1)
        }
      }
    }
    val throttler = system.actorOf(Props(new ThrottlingServiceImpl(2, slaCountedService)))

    throttler ! IsRequestAllowed(Some("t"))
    throttler ! IsRequestAllowed(Some("t"))
    throttler ! IsRequestAllowed(Some("t"))
    (throttler ? IsRequestAllowed(Some("t"))) map {_ => assert(counter == 1)}


  }
}
