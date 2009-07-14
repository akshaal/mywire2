
package info.akshaal.mywire2.daemon

import info.akshaal.mywire2.actor.{MywireActor, LowPriorityPool}

object DeamonStatusActor extends MywireActor with LowPriorityPool {
    schedule payload 'DoIt every 30 seconds

    def act () = {
        case 'DoIt => {
            // TODO: Implement it
        }
    }
}
