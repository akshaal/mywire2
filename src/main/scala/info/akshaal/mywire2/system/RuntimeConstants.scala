/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system

import java.util.Properties

import mywire2.Predefs._

private[system] object RuntimeConstants {
    /**
     * For each thread pool number of threads created is equal to
     * nunber of processes multiplied to this constant.
     */
    val threadsMultiplier = 1

    /**
     * Take average from that number of timings measurements.
     */
    val timingFrameSize = 100

    // ////////// LOW PRIORITY

    /**
     * OS priority thread for low priority threads
     */
    val lowPriorityThreadOSPriority = PrefLoader.getInt ("mywire.os.priority.low")

    /**
     * Show warning if hi priority actor completed its job in
     * time exceeding this limit.
     */
    val warnLowPriorityActorTime = 200.milliseconds

    /**
     * Show warning if latency is higher than this value.
     */
    val warnLowPriorityActorLatency = 600.milliseconds

    // ////////// NORMAL PRIORITY

    /**
     * OS priority thread for hi priority threads
     */
    val normalPriorityThreadOSPriority = PrefLoader.getInt ("mywire.os.priority.normal")

    /**
     * Show warning if hi priority actor completed its job in
     * time exceeding this limit.
     */
    val warnNormalPriorityActorTime = 20.milliseconds

    /*
     * Show warning if latency is higher than this value.
     */
    val warnNormalPriorityActorLatency = 60.milliseconds

    // ////////// HI PRIORITY

    /**
     * OS priority thread for hi priority threads
     */
    val hiPriorityThreadOSPriority = PrefLoader.getInt ("mywire.os.priority.high")

    /**
     * Show warning if hi priority actor completed its job in
     * time exceeding this limit.
     */
    val warnHiPriorityActorTime = 1.milliseconds

    /**
     * Show warning if latency is higher than this value.
     */
    val warnHiPriorityActorLatency = 3.milliseconds

    // ////////// SCHEDULER

    /**
     * Show warning if latency is higher than this value.
     */
    val warnSchedulerLatency = 200.microseconds

    /**
     * If an event should be processed in this or smaller number of nanoseconds,
     * then the event will be processed immidiately.
     */
    val schedulerDrift = 1.microseconds

    // //////////// MONITORING

    /**
     * How many seconds passes between monitoring check for actors.
     * If some actor doesn't respond with this time, then application
     * is considered as broken and will be restarted.
     */
    val actorsMonitoringInterval = 2.seconds

    /**
     * Interval between update of the daemon status.
     */
    val daemonStatusUpdateInterval = 5.seconds

    /**
     * Ignore time measurements (latencies) in first moments after the start
     */
    val ignoreTimingsFor = 1.seconds

    /**
     * Encoding to read and write files in.
     */
    val fileEncoding = "UTF-8"

    /**
     * Load preferences from property file.
     */
    private object PrefLoader {
        val properties = new Properties ()

        withCloseableIO {
            convertNull (this.getClass.getResourceAsStream ("/mywire.properties")) {
                throw new IllegalArgumentException ("File mywire.properties not found")
            }
        } {
            properties.load (_)
        }
    
        private def getString (name : String) : String = {
            convertNull (properties.getProperty (name)) {
                throw new IllegalArgumentException ("Property "
                                                    + name
                                                    + " is required")
            }
        }

        def getInt (name : String) : Int = {
            Integer.valueOf(getString (name)).intValue
        }
    }
}