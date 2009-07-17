
package info.akshaal.mywire2.daemon

import info.akshaal.mywire2.RuntimeConstants
import info.akshaal.mywire2.actor.LowPriorityActor

object DeamonStatusActor extends LowPriorityActor {
    schedule payload 'DoIt every RuntimeConstants.daemonStatusUpdateInterval

    def act () = {
        case 'DoIt => {
            // TODO: Implement it
        }
    }
}
