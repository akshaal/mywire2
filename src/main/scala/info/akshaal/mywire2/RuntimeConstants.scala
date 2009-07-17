/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2

import java.util.Properties

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
    val lowPriorityThreadOSPriority = PrefLoader.getInt ("mywire.os.priority.low")

    /**
     * OS priority thread for hi priority threads
     */
    val hiPriorityThreadOSPriority = PrefLoader.getInt ("mywire.os.priority.high")

    /**
     * Show warning if latency is higher than this value.
     */
    val warnLatency = 100.milliseconds

    /**
     * Show warning if actor completed its job in time exceeding this limit.
     */
    val warnActorTime = 10.milliseconds

    /**
     * If an event should be processed in this or smaller number of nanoseconds,
     * then the event will be processed immidiately.
     */
    val schedulerDrift = 1.microseconds

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