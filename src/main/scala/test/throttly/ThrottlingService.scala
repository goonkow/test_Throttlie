package test.throttly

import akka.actor.Actor

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContextExecutor


case class IsRequestAllowed(token: Option[String])

trait ThrottlingService extends Actor{
  type Cache = Map[Option[String], Sla]
  val graceRps:Int // configurable
  val slaService: SlaService // use mocks/stubs for testing
  // Should return true if the request is within allowed RPS.
  def isRequestAllowed(token:Option[String]): Boolean

}

class ThrottlingServiceImpl(val graceRps: Int, val slaService: SlaService)(implicit ec: ExecutionContextExecutor) extends ThrottlingService {
  val log = new LogImpl
  var cache: Cache = Map()

  def isRequestAllowed(token:Option[String]): Boolean = {
    val cacheSla: Option[Sla] = cache get token
    log.addEvent(cacheSla)

    if (cacheSla.isEmpty) {
      token.foreach{ t =>
        slaService.getSlaByToken(t) onSuccess  {
          case sla => {
            // update cache & log
          }
        }
      }
    }
    log.isExceed(cacheSla)
  }
  override def receive = {
    case IsRequestAllowed(token) => sender ! isRequestAllowed(token)
  }
}
trait Log {
  def addEvent(sla: Option[Sla]): Unit
  def isExceed(sla: Option[Sla]): Boolean
}

class LogImpl extends Log {
  type Time = Long
  var timeLog = scala.collection.mutable.Map[Option[Sla], ListBuffer[Time]]()
  def addEvent(sla: Option[Sla]): Unit = {
    val timeBuffer: ListBuffer[Time] = timeLog.getOrElseUpdate(sla, ListBuffer())
    timeBuffer.append(System.currentTimeMillis)
    if (sla.isDefined && sla.get.rps < timeBuffer.size)
      timeBuffer.remove(0)
  }
  def isExceed(sla: Option[Sla]) = {
    val log: ListBuffer[Time] = timeLog(sla)
    if (log.isEmpty)
      true
    else
      (System.currentTimeMillis - log.head) < 1000
  }
}
