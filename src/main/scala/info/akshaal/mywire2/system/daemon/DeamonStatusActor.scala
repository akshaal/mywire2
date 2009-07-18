
package info.akshaal.mywire2.system.daemon

import system.RuntimeConstants
import actor.LowPriorityActor

private[daemon] object DeamonStatusActor extends LowPriorityActor {
    schedule payload 'DoIt every RuntimeConstants.daemonStatusUpdateInterval

    def act () = {
        case 'DoIt => {
            // TODO: Implement it
        }
    }
}
