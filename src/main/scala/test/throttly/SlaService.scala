package test.throttly

import akka.actor.Actor
import akka.actor.Actor.Receive

import scala.concurrent.{ExecutionContext, Future}

case class Sla(user:String, rps:Int)

trait SlaService {
  def getSlaByToken(token:String):Future[Sla]
}

class SlaServiceImpl(implicit val ec: ExecutionContext) extends SlaService {
  override def getSlaByToken(token: String): Future[Sla] = {
    Future{
      Sla("pew", 1)
    }
  }
}