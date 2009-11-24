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

import logger.{LogServiceActor, LogServiceAppender}
import integration.JmsIntegrationActor

/**
 * Manager for one instance of mywire system.
 */
trait MywireManager {
    /**
     * Start mywire instance.
     */
    def start : Unit

    /**
     * Stop mywire instance.
     */
    def stop : Unit
}

@Singleton
private[system] final class MywireManagerImpl @Inject() (
                    jacoreManager : JacoreManager,
                    logServiceActor : LogServiceActor,
                    jmsIntegrationActor : JmsIntegrationActor
                ) extends MywireManager
{
    private[this] var stopped = false
    private[this] var started = false

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Init code

    // Run actors
    private[this] val actors =
            List ( logServiceActor
                 , jmsIntegrationActor)

    /** {InheritDoc} */
    override lazy val start : Unit = {
        require (!stopped, "Unable to start MywireManager. MywireManager has been stopped")

        // Set flags
        started = true

        // Init logger
        LogServiceAppender.setService (logServiceActor)

        // Start jacore manager
        jacoreManager.start

        // Start actors
        jacoreManager.startActors (actors)
    }

    /** {InheritDoc} */
    override lazy val stop : Unit = {
        require (started, "Unable to stop MywireManager. MywireManager is not started")

        // Set flags
        stopped = true

        // Stop actors
        jacoreManager.stopActors (actors)

        // Stop jacore
        jacoreManager.stop
        
        // Deinit logger
        LogServiceAppender.unsetService
    }
}
