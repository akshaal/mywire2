/*
 * Schedule.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system
package scheduler

import actor.Actor

/**
 * Abstract schedule item.
 */
private[scheduler] abstract sealed class Schedule (val actor : Actor,
                                                   val payload : Any,
                                                   val nanoTime : Long)
                            extends Comparable[Schedule] with NotNull
{
    def nextSchedule () : Option[Schedule]

    def compareTo (that : Schedule) = nanoTime compare that.nanoTime
}

/**
 * Object represent schedule item which will be run only once.
 */
final private[scheduler] class OneTimeSchedule (actor : Actor,
                                                payload : Any,
                                                nanoTime : Long)
                            extends Schedule (actor, payload, nanoTime)
{
    override def nextSchedule () = None

    override def toString =
        ("OneTimeSchedule(actor=" + actor
         + ", payload=" + payload
         + ", nanoTime=" + nanoTime + ")")
}

/**
 * Object represent schedule item which for recurrent events.
 */
final private[scheduler] class RecurrentSchedule (actor : Actor,
                                                  payload : Any,
                                                  nanoTime : Long,
                                                  period : Long)
                            extends Schedule (actor,
                                              payload,
                                              nanoTime)
{
    override def nextSchedule () = {
        Some (new RecurrentSchedule (actor,
                                     payload,
                                     nanoTime + period,
                                     period))
    }

    override def toString =
        ("RecurrentSchedule(actor=" + actor
         + ", payload=" + payload
         + ", nanoTime=" + nanoTime
         + ", period=" + period + ")")
}