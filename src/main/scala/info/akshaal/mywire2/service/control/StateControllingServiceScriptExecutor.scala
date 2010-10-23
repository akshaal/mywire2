/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package service.control

import info.akshaal.jacore.`package`._

import utils.stupdate.StateUpdateScript

/**
 * This trait provides a way to run scripts.
 *
 * This is part of StateMonitoringService and the reason for existance of this trait is
 * to separate service disabling code from StateMonitoringService making it easier to understand.
 *
 * @param T type of value controlled by state monitoring service
 */
private[control] trait StateControllingServiceScriptExecutor [T] {
    this : StateControllingService[T] =>

    // This trait must not make any assumptions of how many script there are running and so on.
    // In theory it must be possible to run any number of scripts simultaneously.

    /**
     * Start script execution.
     *
     * @param script script to start
     */
    private[control] def startScript (script : StateUpdateScript[T]) {
        executeScriptInstruction (script)
    }

    /**
     * Stop script execution.
     *
     * @param script script to stop
     */
    private[control] def stopScript (script : StateUpdateScript[T]) {
        if (script.isInterrupted) {
            debug ("Script is already interrupted, nothing to stop..")
        } else {
            script.interrupt ()

            onScriptFinished (script, true)
        }
    }

    /**
     * Called when a script is finished its execution no matter normally or abnormally (interrupted).
     *
     * @param script script that has finished its execution
     * @param interrupted true if script was interrupted
     */
    private[control] def onScriptFinished (script : StateUpdateScript[T],
                                           interrupted : Boolean) : Unit

    /**
     * Make one step in script. This method does nothing is the script is interrupted.
     *
     * @param script script to run
     */
    private def executeScriptInstruction (script : StateUpdateScript[T]) {
        if (script.isInterrupted) {
            debug ("Script is interrupted. Do nothing")
            return
        }

        val instruction = script.nextInstruction ()

        debugLazy ("Current instruction to run: " + instruction)

        instruction match {
            case script.End =>
                onScriptFinished (script, false)

            case script.SetState (state) =>
                setStateAsy (state) {
                    case result @ Success (_) =>
                        executeScriptInstruction (script)

                    case result @ Failure (msg, excOption) =>
                        stopScript (script)
                }

            case script.Wait (time) =>
                schedule in time executionOf {
                    executeScriptInstruction (script)
                }
        }
    }
}
