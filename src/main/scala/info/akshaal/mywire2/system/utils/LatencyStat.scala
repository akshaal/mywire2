/*
 * LatencyStat.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.utils

import system.RuntimeConstants
import logger.Logger

/**
 * Help trace latency.
 */
private[system] final class LatencyStat extends NotNull {
    private val frame =
        new LongValueFrame (RuntimeConstants.latencyStateFrameSize)

    @inline
    def measureNano (exp : Long) : Long = {
        if (System.nanoTime < LatencyStat.allowedAfter) {
            return 0L
        }

        val latency = LatencyStat.calculateLatencyNano (exp)
        frame.put (latency)
        latency
    }

    @inline
    def getNano () : Long = frame.average()
}

private[system] object LatencyStat {
    private[utils] val allowedAfter =
        System.nanoTime + RuntimeConstants.ignoreLatencyStatsTime.asNanoseconds

    @inline
    def expectationInNano (nano : Long) : Long =
        System.nanoTime() + nano

    @inline
    def expectationInMicro (micro : Long) : Long =
        expectationInNano(micro * 1000L)

    @inline
    def expectationInMili (mili : Long) : Long =
        expectationInNano(mili * 1000000L)

    @inline
    def expectationInSeconds (mili : Long) : Long =
        expectationInNano(mili * 1000000000L)

    @inline
    def calculateLatencyNano (exp : Long) =
        System.nanoTime - exp

    @inline
    def inform (logger : Logger,
                message : => String,
                allowedLatency : Long,
                latency : Long) : Unit =
    {
        if (System.nanoTime < LatencyStat.allowedAfter) {
            return;
        }

        if (latency > allowedLatency) {
            logger.warn (message + ". Latency: " + latency + " ns")
        } else {
            logger.debugLazy (message + ". Latency: " + latency + " ns")
        }
    }
}
