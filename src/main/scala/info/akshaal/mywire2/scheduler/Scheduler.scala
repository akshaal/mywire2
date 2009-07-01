/*
 * Scheduler.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.scheduler

import info.akshaal.mywire2.logger.Logger
import info.akshaal.mywire2.actor.{MywireActor, HiSpeedPool}

final object Scheduler extends MywireActor with HiSpeedPool {
    val logger = Logger.get

    def act () = {
        case schedule : Schedule =>
            println (schedule)
    }

    // Will be started as soon as something is about to be scheduled
    start ()
}

final class Schedule (val actor : MywireActor, val time : Long)
                                extends Ordered[Schedule]
{
    def compare (that : Schedule) = time compare that.time
}
