package test.throttly


import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

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

case class Sla(user:String, rps:Int)

trait SlaService {
  def getSlaByToken(token:String):Future[Sla]
}

trait ThrottlingService {
  type Cache = Map[Option[String], Sla]
  val graceRps:Int // configurable
  val slaService: SlaService // use mocks/stubs for testing
  // Should return true if the request is within allowed RPS.
  def isRequestAllowed(token:Option[String])(log: Log)(implicit cache: Cache): Boolean
}

class ThrottlingServiceImpl(val graceRps: Int, val slaService: SlaService) extends ThrottlingService {
  def isRequestAllowed(token:Option[String])(log: Log)(implicit cache: Cache): Boolean = {
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
}
