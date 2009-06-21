package info.akshaal.mywire2.logger

import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent

class LogServiceAppender extends AppenderSkeleton {
    override def append (event : LoggingEvent) = {
        println ("LogServiceAppender: " + event.getRenderedMessage);
    } 
    
    override def close () = {}
    
    override def requiresLayout = true
}
