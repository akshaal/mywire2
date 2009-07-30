
package info.akshaal.mywire2
package system
package daemon

import Predefs._
import actor.Actor
import utils.{TimeUnit, NormalPriorityPool}

private[system] abstract class DeamonStatusActor extends Actor {
    schedule payload UpdateStatus every interval

    protected val pool : NormalPriorityPool

    protected val interval : TimeUnit

    protected val daemonStatus : DaemonStatus

    protected val statusFile : String

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