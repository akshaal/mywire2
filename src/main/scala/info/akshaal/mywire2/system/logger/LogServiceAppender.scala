package info.akshaal.mywire2
package system
package logger

import java.util.LinkedList
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent

import scala.collection.JavaConversions._

/**
 * Class used by log4j as a custom appender.
 */
final class LogServiceAppender extends AppenderSkeleton {
    import LogServiceAppender._

    override def append (event : LoggingEvent) = {
        val tuple = (event, System.nanoTime)

        logActor match {
            case None =>
                addPending (tuple)

            case Some (actor) =>
                actor ! tuple
        }
    }
    
    override def close () = {}
    
    override def requiresLayout = false
}

private[system] object LogServiceAppender {
    private val MAX_PENDING = 1000

    // All operations on this list must be synchronized
    private val pending = new LinkedList[(LoggingEvent, Long)]

    private var logActor : Option[LogActor] = None

    /**
     * Add pending msg.
     */
    private def addPending (tuple : (LoggingEvent, Long)) : Unit = {
        pending synchronized {
            logActor match {
                case None =>
                    pending.addLast (tuple)
                    if (pending.size > MAX_PENDING) {
                        pending.removeFirst ()
                    }

                case Some (actor) =>
                    actor ! tuple
            }
        }
    }

    /**
     * Set actor that will receive a tuples with event and time when message occured.
     * @param actor actor
     */
    def setActor (actor : LogActor) : Unit = {
        synchronized {
            pending.foreach (tuple => actor ! tuple)

            logActor = Some (actor)
        }
    }

    /**
     * After calling this method, no actor will receive log events, until setActor is called.
     */
    def unsetActor () : Unit = logActor = {
        None
    }
}