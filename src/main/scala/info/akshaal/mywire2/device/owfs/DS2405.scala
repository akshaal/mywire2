/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device
package owfs

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.Operation

import utils.container.{ReadableStateContainer, RWStateContainer}

/**
 * Addressable swtich.
 * @param id unique 1-wire identifier
 * @param deviceEnv device environment
 */
class DS2405 (id : String) (implicit parentDevLoc : DeviceLocation, deviceEnv : OwfsDeviceEnv)
                                extends OwfsDeviceActor (id, "05", parentDevLoc, deviceEnv)
{
    /**
     * A name of file with PIO state.
     */
    protected final val pioFileName : String = "PIO"

    /**
     * A name of file with sensed state.
     */
    protected final val sensedFileName : String = "sensed"

    /**
     * Parse state.
     */
    protected def parseState (state : String) : Boolean =
        state match {
            case "0" => false

            case "1" => true

            case x => throw new NumberFormatException ("Unknown state: " + x)
        }

    /**
     * Get state from file.
     */
    protected def opGetStateFromFile (file : String) : Operation.WithResult [Boolean] =
        opReadAndParse (file, parseState)

    /**
     * Set state in file.
     */
    protected def opSetStateToFile (file : String, state : Boolean) : Operation.WithResult [Unit] =
        opWrite (file, if (state) "1" else "0")

    /**
     * PIO object.
     */
    object PIO extends RWStateContainer[Boolean] {
        /**
         * Async operation to read current PIO state. This is state that was set previously.
         * This is not sensed state (actual state).
         */
        override def opGetState () : Operation.WithResult [Boolean] =
                  opGetStateFromFile (pioFileName)

        /**
         * Write new state for the PIO.
         */
        override def opSetState (state : Boolean) : Operation.WithResult [Unit] =
                  opSetStateToFile (pioFileName, state)
    }

    object Sensed extends ReadableStateContainer[Boolean] {
        /**
         * Async operation to read current PIO state. This is state that was set previously.
         * This is not sensed state (actual state).
         */
        override def opGetState () : Operation.WithResult [Boolean] =
                  opGetStateFromFile (sensedFileName)
    }
}
