package info.akshaal.mywire2
package logger

import java.util.{LinkedList, Date}
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent
import org.apache.log4j.Level

import scala.collection.JavaConversions._

import domain.LogRecord

/**
 * Class used by log4j as a custom appender.
 */
final class LogServiceAppender extends AppenderSkeleton {
    import LogServiceAppender._

    override def append (event : LoggingEvent) = {
        val nano = System.nanoTime
        val stack = event.getThrowableStrRep
        val stackStr = if (stack == null) "" else stack.mkString("\n")
        val levelId = levelToLevelId (event.getLevel)

        val logRecord = LogRecord (time      = new Date (event.getTimeStamp),
                                   nano      = nano,
                                   levelId   = levelId,
                                   category  = event.getLoggerName,
                                   msg       = event.getRenderedMessage,
                                   thread    = event.getThreadName,
                                   throwable = stackStr)

        logService match {
            case None =>
                addPending (logRecord)

            case Some (service) =>
                service.log (logRecord)
        }
    }
    
    override def close () = {}
    
    override def requiresLayout = false

    private[this] def levelToLevelId (level : Level) = level match {
        case Level.DEBUG => LogRecord.debugId
        case Level.INFO  => LogRecord.infoId
        case Level.WARN  => LogRecord.warnId
        case Level.ERROR => LogRecord.errorId
        case level       => throw new IllegalArgumentException ("Unsupported level: " + level)
    }
}

private[mywire2] object LogServiceAppender {
    private val MAX_PENDING = 1000

    // All operations on this list must be synchronized
    private val pending = new LinkedList [LogRecord]

    private var logService : Option[LogService] = None

    /**
     * Add pending msg.
     */
    private def addPending (logRecord : LogRecord) : Unit = {
        pending synchronized {
            logService match {
                case None =>
                    pending.addLast (logRecord)
                    if (pending.size > MAX_PENDING) {
                        pending.removeFirst ()
                    }

                case Some (service) =>
                    service.log (logRecord)
            }
        }
    }

    /**
     * Set service that will receive a tuples with event and time when message occured.
     * @param service service
     */
    def setService (service : LogService) : Unit = {
        synchronized {
            pending.foreach (service.log (_))

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
