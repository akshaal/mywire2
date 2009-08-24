/*
 * MywireManager.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system

import com.google.inject.{Singleton, Inject}

import info.akshaal.jacore.system.JacoreManager
import info.akshaal.jacore.system.actor.Actor

import logger.{LogActor, LogServiceAppender}

@Singleton
final class MywireManager @Inject() (
                    jacoreManager : JacoreManager,
                    logActor : LogActor
                )
{
    private[this] var stopped = false
    private[this] var started = false

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Useful addons

    def startActor (actor : Actor) =
                    jacoreManager.startActor (actor)

    def startActors (it : Iterable[Actor]) = 
                    jacoreManager.startActors (it)

    def stopActor (actor : Actor) =
                    jacoreManager.stopActor (actor)

    def stopActors (it : Iterable[Actor]) =
                    jacoreManager.stopActors (it)

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Init code

    // Run actors
    private[this] val actors =
            (logActor
             :: Nil)

    lazy val start : Unit = {
        require (!stopped,
            "Unable to start MywireManager. MywireManager has been stopped")

        // Set flags
        started = true

        // Init logger
        LogServiceAppender.logActor = Some (logActor)

        // Start jacore manager
        jacoreManager.start

        // Start actors
        startActors (actors)
    }

    lazy val stop : Unit = {
        require (started,
                 "Unable to stop MywireManager. MywireManager is not started")

        // Set flags
        stopped = true

        // Deinit logger
        LogServiceAppender.logActor = None

        // Stop jacore
        jacoreManager.stop

        // Stop actors
        stopActors (actors)
    }
}
