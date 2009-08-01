
package info.akshaal.mywire2
package system
package daemon

import Predefs._
import actor.Actor
import scheduler.Scheduler
import utils.{TimeUnit, NormalPriorityPool}

private[system] final class DeamonStatusActor
                                (pool : NormalPriorityPool,
                                 scheduler : Scheduler,
                                 interval : TimeUnit,
                                 daemonStatus : DaemonStatus,
                                 statusFile : String)
                    extends Actor (pool = pool,
                                   scheduler = scheduler)
{
    schedule payload UpdateStatus every interval

    /**
     * Process messages
     */
    final def act () = {
        case UpdateStatus => {
            // TODO: Implement it
        }
    }

    private final case object UpdateStatus
}