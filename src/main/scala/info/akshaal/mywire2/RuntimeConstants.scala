/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2

import info.akshaal.mywire2.Predefs._

object RuntimeConstants {
    /**
     * For each thread pool number of threads created is equal to
     * nunber of processes multiplied to this constant.
     */
    val threadsMultiplier = 1

    /**
     * Take average from that number of latency measurements.
     */
    val latencyStateFrameSize = 100

    /**
     * OS priority thread for low priority threads
     */
    val lowPriorityThreadOSPriority = 19

    /**
     * OS priority thread for hi priority threads
     */
    var hiPriorityThreadOSPriority = -20

    /**
     * Show warning if latency is higher than this value.
     */
    val warnLatencyNano = 40L * 1000L * 1000L

    /**
     * Show warning if actor completed its job in time exceeding this limit.
     */
    val warnActorTimeNano = 400L * 1000L

    /**
     * If an event should be processed in this or smaller number of nanoseconds,
     * then the event will be processed immidiately.
     */
    val schedulerDriftNano = 300L

    /**
     * How many seconds passes between monitoring check for actors.
     * If some actor doesn't respond with this time, then application
     * is considered as broken and will be restarted.
     */
    val actorsMonitoringInterval = 200.milliseconds

    /**
     * Give some time to the dying application to complete it work as graceful as possible
     */
    val sleepBeforeDie = 1.seconds
}
