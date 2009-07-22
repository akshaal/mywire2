
package info.akshaal.mywire2.system.daemon

import system.RuntimeConstants
import actor.Actor

private[daemon] abstract class DeamonStatusActor extends Actor {
    schedule payload 'DoIt every RuntimeConstants.daemonStatusUpdateInterval

    final def act () = {
        case 'DoIt => {
            // TODO: Implement it
        }
    }
}
