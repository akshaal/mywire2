package info.akshaal.mywire2.system.logger

import org.apache.log4j.spi.LoggingEvent
import org.apache.log4j.Level

import java.util.Date

import actor.Actor
import dao.LogDao
import domain.LogRecord
import scheduler.Scheduler
import utils.LowPriorityPool

/**
 * Logs message.
 */
private[system] abstract class LogActor extends Actor with DummyLogging
{
    protected override val pool : LowPriorityPool

    // ///////////////////////////////////////////////////////////////////

    final def act () = {
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

            LogDao.insertRecord (logRecord)
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