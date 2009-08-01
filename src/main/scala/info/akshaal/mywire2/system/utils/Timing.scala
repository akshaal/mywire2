package info.akshaal.mywire2
package system
package utils

import Predefs._
import logger.Logger
import daemon.DaemonStatus

/**
 * Help measure time.
 */
private[system] final class Timing (limit : TimeUnit,
                                    daemonStatus : DaemonStatus,
                                    prefs : Prefs)
                        extends NotNull
{
    private[this] val valuesNumber = 100
    private[this] val frame = new LongValueFrame (valuesNumber)
    private[this] val allowedAfter =
        (daemonStatus.startedAt + prefs.getTimeUnit ("mywire.timing.skip.first"))
            .asNanoseconds

    private[this] def measure (startNano : Long,
                               logger : Logger) (message : => String) =
    {
        if (startNano > allowedAfter) {
            val stopNano = System.nanoTime
            val time = stopNano - startNano
            frame.put (time)

            // Inform
            if (time > limit.asNanoseconds) {
                logger.warn(message + ". Timing = " + time.nanoseconds)
            } else {
                logger.debugLazy(message + ". Timing = " + time.nanoseconds)
            }
        }
    }

    /**
     * Measure time passed since <code>startNano<code> til now.
     */
    @inline
    def finishedButExpected (startNano : Long, message : => String)
                            (implicit logger : Logger) =
    {
        measure (startNano, logger) (message)
    }

    /**
     * Start measure time and return a function that must be called
     * when timing is done.
     */
    @inline
    def createFinisher (implicit logger : Logger) = {
        // Get current time
        val startNano = System.nanoTime

        // Return function
        measure (startNano, logger) _
    }

    /**
     * Get average timing.
     */
    @inline
    def average : TimeUnit = frame.average().nanoseconds
}