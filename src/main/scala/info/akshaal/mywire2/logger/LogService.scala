package info.akshaal.mywire2
package logger

import com.google.inject.{Singleton, Inject}

import org.apache.ibatis.session.SqlSessionFactory

import info.akshaal.jacore.annotation.CallByMessage
import info.akshaal.jacore.actor.{Actor, LowPriorityActorEnv, NormalPriorityActorEnv}
import info.akshaal.jacore.dao.ibatis.AbstractIbatisDataInserterActor
import info.akshaal.jacore.logger.DummyLogging

import annotation.LogDB
import domain.LogRecord

/**
 * Service that logs message to database.
 */
private[mywire2] trait LogService {
    /**
     * Log event that happened at the given time.
     * @param record record to log
     * @param nano (relative) time of event in nanosecond
     */
    def log (record : LogRecord) : Unit
}

/**
 * Log messages.
 */
@Singleton
private[mywire2] class LogServiceActor @Inject() (
                                    normalPriorityActorEnv : NormalPriorityActorEnv,
                                    logRecordInserted : LogRecordInserterActor)
                    extends Actor (normalPriorityActorEnv)
                    with DummyLogging
                    with LogService
{
    manage (logRecordInserted)

    /** {InheritDoc} */
    @CallByMessage
    override def log (record : LogRecord) : Unit = {
        logRecordInserted.insert (record)
    }
}

/**
 * Data inserter for LogRecord object. This is a slave for LogActor.
 */
@Singleton
private[logger] class LogRecordInserterActor @Inject() (
                                            @LogDB sqlSessionFactory : SqlSessionFactory,
                                            lowPriorityActorEnv : LowPriorityActorEnv)
                        extends AbstractIbatisDataInserterActor[LogRecord] (
                                            sqlSessionFactory = sqlSessionFactory,
                                            lowPriorityActorEnv = lowPriorityActorEnv)
                        with DummyLogging
{
    protected override val insertStatementId = "info.akshaal.mywire2.log.insertLogRecord"
}
