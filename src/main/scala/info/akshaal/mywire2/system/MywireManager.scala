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

    final def startActors (it : Iterable[Actor]) = 
                    jacoreManager.startActors (it)

    final def stopActors (it : Iterable[Actor]) =
                    jacoreManager.stopActors (it)

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Init code

    final lazy val start : Unit = {
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
