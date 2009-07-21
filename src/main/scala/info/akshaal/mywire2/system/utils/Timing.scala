package info.akshaal.mywire2.system.utils

import mywire2.Predefs._
import system.RuntimeConstants
import logger.Logger

/**
 * Help measure time.
 */
private[system] final class Timing (limit : TimeUnit) extends NotNull {
    private[this] val frame =
        new LongValueFrame (RuntimeConstants.timingFrameSize)

    private[this] def measure (startNano : Long,
                               logger : Logger) (message : => String) =
    {
        if (startNano > Timing.allowedAfter) {
            val stopNano = System.nanoTime
            val time = stopNano - startNano
            frame.put (time)

            // Inform
            if (time > limit.asNanoseconds) {
                logger.warn(message + ". Timing(ns) = " + time)
            } else {
                logger.debugLazy(message + ". Timing(ns) = " + time)
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

private[utils] object Timing {
    private[utils] val allowedAfter =
        System.nanoTime + RuntimeConstants.ignoreTimingsFor.asNanoseconds
}
