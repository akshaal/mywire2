package info.akshaal.mywire2
package system
package logger

import java.util.LinkedList
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent

import scala.collection.JavaConversions._

/**
 * Class used by log4j as a custom appender.
 */
final class LogServiceAppender extends AppenderSkeleton {
    import LogServiceAppender._

    override def append (event : LoggingEvent) = {
        logService match {
            case None =>
                addPending (event, System.nanoTime)

            case Some (service) =>
                service.log (event, System.nanoTime)
        }
    }
    
    override def close () = {}
    
    override def requiresLayout = false
}

private[system] object LogServiceAppender {
    private val MAX_PENDING = 1000

    // All operations on this list must be synchronized
    private val pending = new LinkedList[(LoggingEvent, Long)]

    private var logService : Option[LogService] = None

    /**
     * Add pending msg.
     */
    private def addPending (tuple : (LoggingEvent, Long)) : Unit = {
        pending synchronized {
            logService match {
                case None =>
                    pending.addLast (tuple)
                    if (pending.size > MAX_PENDING) {
                        pending.removeFirst ()
                    }

                case Some (service) =>
                    service.log (tuple._1, tuple._2)
            }
        }
    }

    /**
     * Set service that will receive a tuples with event and time when message occured.
     * @param service service
     */
    def setService (service : LogService) : Unit = {
        synchronized {
            pending.foreach (tuple => service.log (tuple._1, tuple._2))

            logService = Some (service)
        }
    }

    /**
     * After calling this method, no service will receive log events, until setService is called.
     */
    def unsetService () : Unit = {
        logService = None
    }
}