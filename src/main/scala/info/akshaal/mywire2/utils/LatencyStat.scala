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
class LatencyStat {
    type Expectation = Long

    private val frame =
        new LongValueFrame (RuntimeConstants.latencyStateFrameSize)

    final def expectationInNano (nano : Long) : Expectation =
        System.nanoTime() + nano

    final def expectationInMicro (micro : Long) : Expectation =
        expectationInNano(micro * 1000L)

    final def expectationInMili (mili : Long) : Expectation =
        expectationInNano(mili * 1000000L)

    final def expectationInSeconds (mili : Long) : Expectation =
        expectationInNano(mili * 1000000000L)

    final def measure (exp : Expectation) = frame.put (System.nanoTime() - exp)

    final def getNano () : Long = frame.average()
}
