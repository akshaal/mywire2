/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system.utils

import info.akshaal.mywire2.Predefs._

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

import ThreadPriorityChanger.{HiPriority, NormalPriority, LowPriority}

/**
 * Hi priority pool.
 */
private[system] final class HiPriorityPool
                           (threads : Int,
                            latencyLimit : TimeUnit,
                            executionLimit : TimeUnit,
                            threadPriorityChanger : ThreadPriorityChanger)
          extends Pool (name = "HiPriorityPool",
                        priority = HiPriority,
                        threads = threads,
                        latencyLimit = latencyLimit,
                        executionLimit = executionLimit,
                        threadPriorityChanger = threadPriorityChanger)

/**
 * Normal priority pool.
 */
private[system] final class NormalPriorityPool
                           (threads : Int,
                            latencyLimit : TimeUnit,
                            executionLimit : TimeUnit,
                            threadPriorityChanger : ThreadPriorityChanger)
          extends Pool (name = "NormalPriorityPool",
                        priority = NormalPriority,
                        threads = threads,
                        latencyLimit = latencyLimit,
                        executionLimit = executionLimit,
                        threadPriorityChanger = threadPriorityChanger)

private[system] final class LowPriorityPool
                           (threads : Int,
                            latencyLimit : TimeUnit,
                            executionLimit : TimeUnit,
                            threadPriorityChanger : ThreadPriorityChanger)
          extends Pool (name = "LowPriorityPool",
                        priority = LowPriority,
                        threads = threads,
                        latencyLimit = latencyLimit,
                        executionLimit = executionLimit,
                        threadPriorityChanger = threadPriorityChanger)

/**
 * Pool class to be used by actors.
 */
private[system] abstract sealed class Pool
                            (name : String,
                             priority : ThreadPriorityChanger.Priority,
                             threads : Int,
                             latencyLimit : TimeUnit,
                             executionLimit : TimeUnit,
                             threadPriorityChanger : ThreadPriorityChanger)
{
    final val latencyTiming = new Timing (latencyLimit)
    final val executionTiming = new Timing (latencyLimit)

    private val threadFactory = new ThreadFactory {
        val counter = new AtomicInteger (0)

        def newThread (r : Runnable) : Thread = {
            val threadNumber = counter.incrementAndGet

            val proxy = mkRunnable {
                threadPriorityChanger.change (priority)
                r.run
            }

            val thread = new Thread (proxy)
            thread.setName (name + "-" + threadNumber)
            
            thread
        }
    }

    final val executors = Executors.newFixedThreadPool (threads, threadFactory)
}
