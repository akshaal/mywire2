/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.utils

import info.akshaal.mywire2.Predefs._

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

/**
 * Pool itself for low priority execution.
 */
object LowPriorityPool extends Pool ("LowPriorityPool",
                                     ThreadPriorityChanger.LowPriority ())

/**
 * Pool for hi priority actors.
 */
object HiPriorityPool extends Pool ("HiPriorityPool",
                                    ThreadPriorityChanger.HiPriority ())
/**
 * Pool class to be used by actors.
 */
sealed class Pool (name : String,
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