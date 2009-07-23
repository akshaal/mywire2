/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system.utils

import mywire2.Predefs._

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

import ThreadPriorityChanger.{HiPriority, NormalPriority, LowPriority}

private[system] trait HiPriorityPool extends {
    private[utils] override final val name = "HiPriorityPool"

    private[utils] override final val priority = HiPriority
} with Pool

private[system] trait NormalPriorityPool extends {
    private[utils] override final val name = "NormalPriorityPool"

    private[utils] override final val priority = NormalPriority
} with Pool

private[system] trait LowPriorityPool extends {
    private[utils] override final val name = "LowPriorityPool"

    private[utils] override final val priority = LowPriority
} with Pool

/**
 * Pool class to be used by actors.
 */
private[system] trait Pool
{
    private[utils] val name : String

    private[utils] val priority : ThreadPriorityChanger.Priority

    val threads : Int

    val latencyLimit : TimeUnit

    val executionLimit : TimeUnit

    // -- - - - - -  - -- -- - -  - - - - - - --  - -- - - -
    // Concrete

    final val latencyTiming = new Timing (latencyLimit)
    final val executionTiming = new Timing (latencyLimit)

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

    final val executors = Executors.newFixedThreadPool (threads, threadFactory)
}
