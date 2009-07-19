/*
 * SchedulerThread.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.scheduler

import java.util.concurrent.locks.ReentrantLock
import java.util.PriorityQueue

import mywire2.Predefs._
import system.RuntimeConstants
import logger.Logging
import utils.{LatencyStat, ThreadPriorityChanger}

private[scheduler] object SchedulerThread extends Thread with Logging {
    @volatile
    private var shutdownFlag = false
    private val lock = new ReentrantLock
    private val condition = lock.newCondition
    private val queue = new PriorityQueue[Schedule]
    private val latencyStat = new LatencyStat

    def schedule (item : Schedule) = {
        synchronized {
            queue.offer (item)

            // We need to reschedule thread if we added something to the
            // head of the list
            if (queue.peek eq item) {
                locked { condition.signal () }
            }
        }
    }

    def getLatencyNano () = latencyStat.getNano

    def shutdown () = {
        shutdownFlag = true
        locked { condition.signal () }
    }

    override def run () {
        info ("Starting scheduler")
        this.setName("Scheduler")

        ThreadPriorityChanger.change (ThreadPriorityChanger.HiPriority)

        // Main loop
        while (!shutdownFlag) {
            logIgnoredException (logger,
                                 "Ignored exception during wait and process")
            {
                waitAndProcess
            }
        }

        // Bye-bye
        info ("Stopping scheduler")
    }

    private def waitAndProcess () = {
        val item = synchronized { queue.peek }

        if (item == null) {
            // No items to process, sleep until signal
            locked { condition.await }
        } else {
            val delay = item.nanoTime - System.nanoTime

            if (delay < RuntimeConstants.schedulerDrift.asNanoseconds) {
                locked { processFromHead }
            } else {
                locked { condition.awaitNanos (delay) }
            }
        }
    }

    private def processFromHead () = {
        // Get item from head
        val item = synchronized { queue.poll }

        // Measure latency
        val latency = latencyStat.measureNano(item.nanoTime)
        LatencyStat.inform (logger,
                            "Event triggered: " + item,
                            RuntimeConstants.warnSchedulerLatency.asNanoseconds,
                            latency)

        // Send message to actor
        item.actor ! (TimeOut (item.payload))

        // Reschedule if needed
        item.nextSchedule match {
            case None => ()
            case Some (nextItem) =>
                schedule (nextItem)
        }
    }

    @inline
    private def locked[T] (code : => T) : T = {
        lock.lock
        try {
            code
        } finally {
            lock.unlock
        }
    }
}
