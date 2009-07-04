/*
 * Schedule.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.scheduler

import info.akshaal.mywire2.actor.MywireActor

/**
 * Abstract schedule item.
 */
private[scheduler] abstract sealed class Schedule (val actor : MywireActor,
                                                   val payload : Any,
                                                   val nanoTime : Long)
                            extends Comparable[Schedule]
{
    def nextSchedule () : Option[Schedule]

    def compareTo (that : Schedule) = nanoTime compare that.nanoTime
}

/**
 * Object represent schedule item which will be run only once.
 */
final private[scheduler] class OneTimeSchedule (actor : MywireActor,
                                                payload : Any,
                                                nanoTime : Long)
                            extends Schedule (actor, payload, nanoTime)
{
    override def nextSchedule () = None
}
