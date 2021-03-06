package test.throttly

import akka.actor.Actor

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, SynchronizedSet}
import scala.concurrent.ExecutionContextExecutor


case class IsRequestAllowed(token: Option[String])

trait ThrottlingService extends Actor{
  type Cache = TrieMap[Option[String], Sla]
  val graceRps:Int // configurable
  val slaService: SlaService // use mocks/stubs for testing
  // Should return true if the request is within allowed RPS.
  def isRequestAllowed(token:Option[String]): Boolean

}

class ThrottlingServiceImpl(val graceRps: Int, val slaService: SlaService)(implicit ec: ExecutionContextExecutor) extends ThrottlingService {
  val log = new LogImpl(graceRps)
  val cache: Cache = TrieMap.empty
  //you should not query the service, if the same token request is already in progress.
  val activeRequests = new mutable.HashSet[String] with SynchronizedSet[String]

  def isRequestAllowed(token:Option[String]): Boolean = {
    val cacheSla: Option[Sla] = cache get token
    if (cacheSla.isEmpty) {
      requestSla(token)
    }
    val result = log.isAllowed(cacheSla)
    log.logEvent(cacheSla)
    result
  }
  def requestSla(token: Option[String]) = {
    token.foreach{ t =>
      if (!activeRequests.contains(t)) {
        activeRequests.add(t)
        slaService.getSlaByToken(t) onSuccess  {
          case sla: Sla =>
            activeRequests.remove(t)
            cache += (Some(t) -> sla)
        }
      }
    }
  }
  override def receive = {
    case IsRequestAllowed(token) => sender ! isRequestAllowed(token)
  }
}
trait Log {
  def logEvent(sla: Option[Sla]): Unit
  def isAllowed(sla: Option[Sla]): Boolean
}

class LogImpl(graceRps: Int) extends Log {
  type Time = Long
  var timeLog = TrieMap[Option[Sla], ListBuffer[Time]](None -> ListBuffer())
  def logEvent(sla: Option[Sla]): Unit = {
    val timeBuffer: ListBuffer[Time] = timeLog.getOrElseUpdate(sla, ListBuffer())
    timeBuffer.append(System.currentTimeMillis)
      if (rps(sla) < timeBuffer.size)
        timeBuffer.remove(0)
  }
  def isAllowed(sla: Option[Sla]): Boolean = {
    if (!timeLog.isDefinedAt(sla))
      return true
    val log: ListBuffer[Time] = timeLog(sla)
    if (log.size < rps(sla))
      return true
    else
      (System.currentTimeMillis - log.head) > 1000
  }
  def rps(sla: Option[Sla]): Int = sla.map(_.rps).getOrElse(graceRps)
}
