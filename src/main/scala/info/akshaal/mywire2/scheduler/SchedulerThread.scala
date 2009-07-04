/*
 * SchedulerThread.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.scheduler

import java.util.concurrent.locks.ReentrantLock
import java.util.PriorityQueue

import info.akshaal.mywire2.RuntimeConstants
import info.akshaal.mywire2.logger.Logging
import info.akshaal.mywire2.utils.{LatencyStat,
                                   ThreadPriorityChanger}

// TODO: Make it stoppable

private[scheduler] object SchedulerThread extends Thread with Logging {
    @volatile
    private var shutdown = false
    private val lock = new ReentrantLock
    private val condition = lock.newCondition
    private val queue = new PriorityQueue[Schedule]

    override def run () {
        info ("Starting scheduler")

        ThreadPriorityChanger.change (ThreadPriorityChanger.HiPriority ())

        // Main loop
        while (!shutdown) {
            waitAndProcess
        }

        // Bye-bye
        info ("Stopping scheduler")
    }

    private def waitAndProcess () = {
        val item = synchronized { queue.peek }

        // TODO: Catch InterruptedException

        if (item == null) {
            // No items to process, sleep until signal
            locked { condition.await }
        } else {
            val delay = item.nanoTime - System.nanoTime

            if (delay < RuntimeConstants.schedulerDriftNano) {
                locked { processFromHead }
            } else {
                locked { condition.awaitNanos (delay) }
            }
        }
    }

    def processFromHead () = {
        // Get item from head
        val item = synchronized { queue.poll }

        // Send message to actor
        item.actor ! item.payload

        // Reschedule if needed
        item.nextSchedule match {
            case None => ()
            case Some (nextItem) =>
                schedule (nextItem)
        }
    }

    private[scheduler] def schedule (item : Schedule) = {
        synchronized {
            queue.offer (item)

            // We need to reschedule thread if we added something to the
            // head of the list
            if (queue.peek eq item) {
                locked { condition.signal () }
            }
        }
    }

    private def locked[T] (code : => T) : T = {
        lock.lock
        try {
            code
        } finally {
            lock.unlock
        }
    }
}
