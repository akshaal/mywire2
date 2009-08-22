
package info.akshaal.mywire2
package system
package daemon

import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named

import Predefs._
import actor.Actor
import scheduler.Scheduler
import utils.{TimeUnit, NormalPriorityPool}

@Singleton
private[system] final class DaemonStatusActor @Inject() (
                 pool : NormalPriorityPool,
                 scheduler : Scheduler,
                 daemonStatus : DaemonStatus,
                 @Named("jacore.status.update.interval") interval : TimeUnit,
                 @Named("jacore.status.file") statusFile : String)
            extends Actor (pool = pool, scheduler = scheduler)
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