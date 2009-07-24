/*
 * ActorManager.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.actor

import system.logger.Logging

private[system] trait ActorManager extends Logging {
    private[system] val monitoring : Monitoring

    final def startActor (actor : Actor) = {
        actor.start
        monitoring.add (actor)
    }

    final def stopActor (actor : Actor) = {
        actor.stop
        monitoring.remove (actor)
    }
}
