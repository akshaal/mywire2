/*
 * MywireManager.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system

import com.google.inject.{Singleton, Inject}

import fs.FileActor
import daemon.DaemonStatusActor
import actor.{ActorManager, MonitoringActors, Actor}
import scheduler.Scheduler
import logger.{LogActor, LogServiceAppender}

@Singleton
class MywireManager @Inject() (
                    logActor : LogActor,
                    fileActor : FileActor,
                    daemonStatusActor : DaemonStatusActor,
                    monitoringActors : MonitoringActors,
                    actorManager : ActorManager,
                    scheduler : Scheduler
                )
{
    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Useful addons

    final def startActors (it : Iterable[Actor]) = {
        it.foreach (actorManager.startActor (_))
    }

    final def stopActors (it : Iterable[Actor]) = {
        it.foreach (actorManager.stopActor (_))
    }

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Init code

    final lazy val start : Unit = {
        // Init logger
        LogServiceAppender.logActor = Some (logActor)

        // Run actors
        val actors =
            (logActor
             :: fileActor
             :: daemonStatusActor
             :: monitoringActors.monitoringActor1
             :: monitoringActors.monitoringActor2
             :: Nil)

        startActors (actors)

        // Start scheduling
        scheduler.start ()
    }
}
