package info.akshaal.mywire2.logger

import org.apache.log4j.{AppenderSkeleton, Layout}
import org.apache.log4j.spi.LoggingEvent

/**
 * Class used by log4j as a custom appender.
 */
final class LogServiceAppender extends AppenderSkeleton {
    override def append (event : LoggingEvent) = {
        LogActor ! (getLayout, event)
    }
    
    override def close () = {}
    
    override def requiresLayout = true
}
