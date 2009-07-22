/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system.utils

import mywire2.Predefs._

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

import ThreadPriorityChanger.{HiPriority, NormalPriority, LowPriority}

private[system] abstract class HiPriorityPool extends Pool {
    private[utils] override final val name = "HiPriorityPool"

    private[utils] override final val priority = HiPriority
}

private[system] abstract class NormalPriorityPool extends Pool {
    private[utils] override final val name = "NormalPriorityPool"

    private[utils] override final val priority = NormalPriority
}

private[system] abstract class LowPriorityPool extends Pool {
    private[utils] override final val name = "LowPriorityPool"

    private[utils] override final val priority = LowPriority
}

/**
 * Pool class to be used by actors.
 */
private[system] sealed abstract class Pool
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