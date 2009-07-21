/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system.utils

import mywire2.Predefs._

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

private[system] final class HiPriorityPool (threads : Int,
                                            latencyLimit : TimeUnit,
                                            executionLimit : TimeUnit)
                extends Pool (name = "HiPriorityPool",
                              priority = ThreadPriorityChanger.HiPriority,
                              threads = threads,
                              latencyLimit = latencyLimit,
                              executionLimit = executionLimit)

private[system] final class NormalPriorityPool (threads : Int,
                                                latencyLimit : TimeUnit,
                                                executionLimit : TimeUnit)
                extends Pool (name = "NormalPriorityPool",
                              priority = ThreadPriorityChanger.NormalPriority,
                              threads = threads,
                              latencyLimit = latencyLimit,
                              executionLimit = executionLimit)

private[system] final class LowPriorityPool (threads : Int,
                                             latencyLimit : TimeUnit,
                                             executionLimit : TimeUnit)
                extends Pool (name = "LowPriorityPool",
                              priority = ThreadPriorityChanger.LowPriority,
                              threads = threads,
                              latencyLimit = latencyLimit,
                              executionLimit = executionLimit)

/**
 * Pool class to be used by actors.
 */
private[system] sealed abstract class Pool (
                            name : String,
                            priority : ThreadPriorityChanger.Priority,
                            threads : Int,
                            latencyLimit : TimeUnit,
                            executionLimit : TimeUnit)
{
    val latencyTiming = new Timing (latencyLimit)
    val executionTiming = new Timing (latencyLimit)

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

    val executors = Executors.newFixedThreadPool (threads, threadFactory)
}