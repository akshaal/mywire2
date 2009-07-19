/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system.utils

import mywire2.Predefs._

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

/**
 * Pool itself for low priority execution.
 */
private[system] object LowPriorityPool
                extends Pool ("LowPriorityPool",
                              ThreadPriorityChanger.LowPriority)

/**
 * Pool for normal priority tasks..
 */
private[system] object NormalPriorityPool
                extends Pool ("NormalPriorityPool",
                              ThreadPriorityChanger.NormalPriority)

/**
 * Pool for hi priority tasks..
 */
private[system] object HiPriorityPool
                extends Pool ("HiPriorityPool",
                              ThreadPriorityChanger.HiPriority)
/**
 * Pool class to be used by actors.
 */
private[system] sealed abstract class Pool (
                                   name : String,
                                   priority : ThreadPriorityChanger.Priority) {
    val latency = new LatencyStat

    private val numberOfThreadInPool =
                    RuntimeConstants.threadsMultiplier * Runtime.getRuntime.availableProcessors

    private val threadFactory = new ThreadFactory {
        val counter = new AtomicInteger (0)

        def newThread (r : Runnable) : Thread = {
            val threadNumber = counter.incrementAndGet

            val proxy = mkRunnable {
                ThreadPriorityChanger.change (priority)
                r.run
            }

            val thread = new Thread (proxy)
            thread.setName (name + "-" + threadNumber)
            
            thread
        }
    }

    val executors =
        Executors.newFixedThreadPool (numberOfThreadInPool, threadFactory);

    final def getLatencyNano () = latency.getNano
}