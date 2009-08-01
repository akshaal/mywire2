/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2
package system
package utils

import Predefs._

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

import ThreadPriorityChanger.{HiPriority, NormalPriority, LowPriority}
import daemon.DaemonStatus

/**
 * Hi priority pool.
 */
private[system] final class HiPriorityPool
                           (threads : Int,
                            latencyLimit : TimeUnit,
                            executionLimit : TimeUnit,
                            prefs : Prefs,
                            daemonStatus : DaemonStatus,
                            threadPriorityChanger : ThreadPriorityChanger)
          extends Pool (name = "HiPriorityPool",
                        prefs = prefs,
                        daemonStatus = daemonStatus,
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
                            prefs : Prefs,
                            daemonStatus : DaemonStatus,
                            latencyLimit : TimeUnit,
                            executionLimit : TimeUnit,
                            threadPriorityChanger : ThreadPriorityChanger)
          extends Pool (name = "NormalPriorityPool",
                        prefs = prefs,
                        daemonStatus = daemonStatus,
                        priority = NormalPriority,
                        threads = threads,
                        latencyLimit = latencyLimit,
                        executionLimit = executionLimit,
                        threadPriorityChanger = threadPriorityChanger)

private[system] final class LowPriorityPool
                           (threads : Int,
                            prefs : Prefs,
                            daemonStatus : DaemonStatus,
                            latencyLimit : TimeUnit,
                            executionLimit : TimeUnit,
                            threadPriorityChanger : ThreadPriorityChanger)
          extends Pool (name = "LowPriorityPool",
                        prefs = prefs,
                        daemonStatus = daemonStatus,
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
                             prefs : Prefs,
                             daemonStatus : DaemonStatus,
                             threadPriorityChanger : ThreadPriorityChanger)
{
    final val latencyTiming = new Timing (limit = latencyLimit,
                                          prefs = prefs,
                                          daemonStatus = daemonStatus)
    final val executionTiming = new Timing (limit = latencyLimit,
                                            prefs = prefs,
                                            daemonStatus = daemonStatus)

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
