package info.akshaal.mywire2
package system
package logger

import com.google.inject.{Singleton, Inject}

import org.apache.log4j.spi.LoggingEvent
import org.apache.log4j.Level

import com.ibatis.sqlmap.client.SqlMapClient

import java.util.Date

import info.akshaal.jacore.system.actor.{Actor, LowPriorityActorEnv, NormalPriorityActorEnv}
import info.akshaal.jacore.system.dao.ibatis.IbatisDataInserterActor
import info.akshaal.jacore.system.logger.DummyLogging

import domain.LogRecord

/**
 * Logs message.
 */
@Singleton
private[system] final class LogActor @Inject() (
                                    normalPriorityActorEnv : NormalPriorityActorEnv,
                                    logRecordInserted : LogRecordInserterActor)
                    extends Actor (normalPriorityActorEnv)
                    with DummyLogging
{
    manage (logRecordInserted)

    override def act () = {
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

            logRecordInserted.insert (logRecord)
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

/**
 * Data inserter for LogRecord object. This is a slave for LogActor.
 */
@Singleton
private[logger] class LogRecordInserterActor @Inject() (
                                            sqlMapClient : SqlMapClient,
                                            lowPriorityActorEnv : LowPriorityActorEnv)
                        extends IbatisDataInserterActor[LogRecord] (
                                            sqlMapClient = sqlMapClient,
                                            lowPriorityActorEnv = lowPriorityActorEnv)
                        with DummyLogging
{
    protected override val insertStatementId = "insertLogRecord"
}