/*
 * LatencyStat.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils

import info.akshaal.mywire2.RuntimeConstants
import info.akshaal.mywire2.logger.Logger

/**
 * Help trace latency.
 */
final class LatencyStat {
    private val frame =
        new LongValueFrame (RuntimeConstants.latencyStateFrameSize)

    def measureNano (exp : Long) = {
        val latency = LatencyStat.calculateLatencyNano (exp)
        frame.put (latency)
        latency
    }

    def getNano () : Long = frame.average()
}

object LatencyStat {
    def expectationInNano (nano : Long) : Long =
        System.nanoTime() + nano

    def expectationInMicro (micro : Long) : Long =
        expectationInNano(micro * 1000L)

    def expectationInMili (mili : Long) : Long =
        expectationInNano(mili * 1000000L)

    def expectationInSeconds (mili : Long) : Long =
        expectationInNano(mili * 1000000000L)

    def calculateLatencyNano (exp : Long) =
        System.nanoTime - exp

    def inform (logger : Logger,
                message : => String,
                allowedLatency : Long,
                latency : Long) =
    {
        if (latency > allowedLatency) {
            logger.warn (message + ". Latency: " + latency + " ns")
        } else {
            logger.debugLazy (message + ". Latency: " + latency + " ns")
        }
    }
}
