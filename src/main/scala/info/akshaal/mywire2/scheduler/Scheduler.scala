/*
 * Scheduler.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.scheduler

import info.akshaal.mywire2.Predefs._
import info.akshaal.mywire2.utils.TimeUnit
import info.akshaal.mywire2.logger.Logging
import info.akshaal.mywire2.actor.MywireActor

/**
 * Scheduler class.
 */
object Scheduler extends Object with Logging {
    SchedulerThread.start

    def in (actor : MywireActor, payload : Any, timeUnit : TimeUnit) =
        SchedulerThread.schedule (new OneTimeSchedule (actor,
                                  payload,
                                  timeUnit.asNanoseconds + System.nanoTime))

    def every (actor : MywireActor, payload : Any, period : TimeUnit) = {
        val periodNano = period.asNanoseconds
        val curNanoTime = System.nanoTime
        val semiStableNumber = actor.getClass.getName.toString.hashCode

        def calc (shift : Long) =
            ((curNanoTime / periodNano + shift) * periodNano
             + semiStableNumber % periodNano)

        val variantOfNanoTime = calc(0)
        val nanoTime =
            if (variantOfNanoTime < curNanoTime) calc(1) else variantOfNanoTime

        SchedulerThread.schedule (new RecurrentSchedule (actor,
                                                         payload,
                                                         nanoTime,
                                                         periodNano))
    }

    def getLatencyNano () = SchedulerThread.getLatencyNano

    def shutdown () = SchedulerThread.shutdown
}

/**
 * Object of this class will be used as a holder of payload when message
 * is delivered.
 */
sealed case class TimeOut (val payload : Any)