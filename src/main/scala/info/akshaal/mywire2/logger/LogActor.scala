package info.akshaal.mywire2.logger

import org.apache.log4j.Layout
import org.apache.log4j.spi.LoggingEvent

import info.akshaal.mywire2.actor.{MywireActor, LowSpeedPool}

/**
 * Logs message.
 */
final object LogActor extends MywireActor with LowSpeedPool with DummyLogging {
    def act () = {
        case (layout : Layout, event : LoggingEvent) => {
            val msg = layout.format(event)

            println ("LogActor: " + msg)
        }
    }

    start ()
}
