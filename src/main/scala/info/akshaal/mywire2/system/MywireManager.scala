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

    lazy val start : Unit = {
        // Init logger
        LogServiceAppender.logActor = Some (logActor)

        // Start jacore manager
        jacoreManager.start

        // Run actors
        val actors =
            (logActor
             :: Nil)

        startActors (actors)
    }
}
