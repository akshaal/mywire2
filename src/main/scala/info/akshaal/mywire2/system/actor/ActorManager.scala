/*
 * ActorManager.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system
package actor

import com.google.inject.{Inject, Singleton}

import logger.Logging

@Singleton
private[system] final class ActorManager @Inject()
                        (monitoring : Monitoring)
                    extends Logging
{
    final def startActor (actor : Actor) = {
        actor.start
        monitoring.add (actor)
    }

    final def stopActor (actor : Actor) = {
        actor.stop
        monitoring.remove (actor)
    }
}
