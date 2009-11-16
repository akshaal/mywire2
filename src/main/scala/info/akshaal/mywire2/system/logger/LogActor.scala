package info.akshaal.mywire2
package system
package logger

import com.google.inject.{Singleton, Inject}

import org.apache.log4j.spi.LoggingEvent
import org.apache.log4j.Level

import java.util.Date

import info.akshaal.jacore.system.actor.{Actor, LowPriorityActorEnv}
import info.akshaal.jacore.system.logger.DummyLogging

import dao.LogDao
import domain.LogRecord

/**
 * Logs message.
 */
@Singleton
private[system] final class LogActor @Inject() (
                                    lowPriorityActorEnv : LowPriorityActorEnv,
                                    logDao : LogDao)
                    extends Actor (lowPriorityActorEnv)
                    with DummyLogging
{
    final override def act () = {
        case (event : LoggingEvent, nano : Long) => {
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

            logDao.insertRecord (logRecord)
        }
    }

    private def levelToLevelId (level : Level) = level match {
        case Level.DEBUG => LogRecord.debugId
        case Level.INFO  => LogRecord.infoId
        case Level.WARN  => LogRecord.warnId
        case Level.ERROR => LogRecord.errorId
        case level =>
            throw new IllegalArgumentException ("Unsupported level: " + level)
    }
}
