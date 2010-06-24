/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils

import info.akshaal.jacore.`package`._

/**
 * Update for a state. Used to keep state update with additional information.
 * @param T type of state
 * @param state new state
 * @param validTime how this state is valid
 */
class StateUpdate[T] (val state : T, val validTime : TimeValue) {
    /**
     * Rerturns true if other object can be equal to this one.
     */
    def canEqual (other : Any) : Boolean = other.isInstanceOf [StateUpdate[_]]

    /**
     * Equality.
     */
    override def equals (other : Any) : Boolean =
        other match {
            case that : StateUpdate[_] =>
                canEqual (other) && state == that.state && validTime == that.validTime

            case _ => false
        }

    /**
     * Hash code
     */
    override def hashCode() = state.hashCode * 41 + validTime.hashCode

    /**
     * To string
     */
    override def toString() = "StateUpdate(state=" + state + ", validTime=" + validTime + ")"
}