/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package utils

import info.akshaal.jacore.`package`._

import info.akshaal.jacore.actor.Operation

/**
 * A state with method to get it and change it.
 */
trait StateContainer [T] {
    /**
     * Operation to get current state.
     */
    def opGetState () : Operation.WithResult [T]

    /**
     * Operation to set current state.
     */
    def opSetState (t : T) : Operation.WithResult [Unit]
}
