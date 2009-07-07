/*
 * Scheduler.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.scheduler

import info.akshaal.mywire2.logger.Logging
import info.akshaal.mywire2.actor.MywireActor

/**
 * Scheduler class.
 */
object Scheduler extends Object with Logging {
    SchedulerThread.start

    def inNano (actor : MywireActor, payload : Any, nano : Long) =
        SchedulerThread.schedule (new OneTimeSchedule (actor,
                                  payload,
                                  nano + System.nanoTime))

    def inMicro (actor : MywireActor, payload : Any, micro : Long) =
        inNano (actor, payload, micro * 1000L)

    def inMili (actor : MywireActor, payload : Any, micro : Long) =
        inNano (actor, payload, micro * 1000000L)

    def inSecs (actor : MywireActor, payload : Any, micro : Long) =
        inNano (actor, payload, micro * 1000000000000L)

    def everyNano (actor : MywireActor, payload : Any, periodNano : Long) = {
        val curNanoTime = System.nanoTime
        val semiStableNumber = actor.getClass.getName.toString.hashCode

        def calc (shift : Long) =
            (curNanoTime / periodNano + shift) * periodNano
                + semiStableNumber % periodNano

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