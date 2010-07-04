package info.akshaal.mywire2
package logger

import java.util.{LinkedList, Date}
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent
import org.apache.log4j.Level

import scala.collection.JavaConversions._

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.logger.Logger

import domain.LogRecord
import domain.LogRecordLevel

/**
 * Class used by log4j as a custom appender.
 */
final class LogServiceAppender extends AppenderSkeleton {
    import LogServiceAppender._

    override def append (event : LoggingEvent) = {
        val nano = System.nanoTime
        val stack = event.getThrowableStrRep
        val stackStr = if (stack == null) "" else stack.mkString("\n")
        val category = event.getLoggerName
        val level = toLogRecordLevel (event.getLevel, category)

        val logRecord =
            LogRecord (time      = new Date (event.getTimeStamp),
                       nano      = nano,
                       level     = level,
                       category  = category,
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

    private[this] def toLogRecordLevel (level : Level, fqcn : String) = level match {
        case Level.DEBUG => LogRecordLevel.Debug

        case Level.INFO  if Logger.isBusinessLogicFQCN(fqcn) => LogRecordLevel.BusinessLogicInfo

        case Level.INFO  => LogRecordLevel.Info

        case Level.WARN  if Logger.isBusinessLogicFQCN(fqcn) => LogRecordLevel.BusinessLogicWarning
            
        case Level.WARN  => LogRecordLevel.Warn

        case Level.ERROR if Logger.isBusinessLogicFQCN(fqcn) => LogRecordLevel.BusinessLogicProblem
            
        case Level.ERROR => LogRecordLevel.Error

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
        synchronized {
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
