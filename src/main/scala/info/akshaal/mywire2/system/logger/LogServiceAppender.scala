package info.akshaal.mywire2.system.logger

import org.apache.log4j.{AppenderSkeleton, Layout}
import org.apache.log4j.spi.LoggingEvent

import actor.Actor

/**
 * Class used by log4j as a custom appender.
 */
final class LogServiceAppender extends AppenderSkeleton {
    override def append (event : LoggingEvent) = {
        LogServiceAppender.logActor match {
            case None =>
                println ("No log actor defined!")

            case Some(actor) =>
                actor ! (event, System.nanoTime)
        }
    }
    
    override def close () = {}
    
    override def requiresLayout = false
}

private[system] object LogServiceAppender {
    var logActor : Option[LogActor] = None
}