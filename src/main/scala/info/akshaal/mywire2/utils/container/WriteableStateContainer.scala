/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils.container

import info.akshaal.jacore.`package`._

import info.akshaal.jacore.actor.Operation

/**
 * A state container with a method to change it.
 */
trait WriteableStateContainer[T] {
    /**
     * Operation to set current state.
     */
    def opSetState (t : T) : Operation.WithResult [Unit]
}
