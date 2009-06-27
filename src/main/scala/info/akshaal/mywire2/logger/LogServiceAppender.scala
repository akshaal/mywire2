package info.akshaal.mywire2.logger

import org.apache.log4j.{AppenderSkeleton, Layout}
import org.apache.log4j.spi.LoggingEvent

import info.akshaal.mywire2.actor.{MywireActor, LowSpeedPool}

/**
 * Class used by log4j as a custom appender.
 */
class LogServiceAppender extends AppenderSkeleton {
    override def append (event : LoggingEvent) = LogActor ! (getLayout, event)
    
    override def close () = {}
    
    override def requiresLayout = true
}

/**
 * Logs message.
 */
object LogActor extends MywireActor with LowSpeedPool {
    def act () = {
        case (layout : Layout, event : LoggingEvent) => {
            val msg = layout.format(event)
            
            println ("LogActor: " + msg)
        }
    }

    start ()
}