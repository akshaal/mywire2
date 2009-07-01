/*
 * LatencyStat.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils

import info.akshaal.mywire2.RuntimeConstants

/**
 * Help trace latency.
 */
final class LatencyStat {
    private val frame =
        new LongValueFrame (RuntimeConstants.latencyStateFrameSize)

    def measureNano (exp : LatencyStat.Expectation) = {
        val latency = LatencyStat.calculateLatencyNano (exp)
        frame.put (latency)
        latency
    }

    def getNano () : Long = frame.average()
}

final object LatencyStat {
    type Expectation = Long

    def expectationInNano (nano : Long) : LatencyStat.Expectation =
        System.nanoTime() + nano

    def expectationInMicro (micro : Long) : LatencyStat.Expectation =
        expectationInNano(micro * 1000L)

    def expectationInMili (mili : Long) : LatencyStat.Expectation =
        expectationInNano(mili * 1000000L)

    def expectationInSeconds (mili : Long) : LatencyStat.Expectation =
        expectationInNano(mili * 1000000000L)

    def calculateLatencyNano (exp : LatencyStat.Expectation) =
        System.nanoTime - exp
}
