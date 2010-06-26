/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils.container

import info.akshaal.jacore.`package`._

import info.akshaal.jacore.actor.Operation

/**
 * A state container with a method to get its value.
 */
trait ReadableStateContainer [T] {
    /**
     * Operation to get current state.
     */
    def opGetState () : Operation.WithResult [T]
}
