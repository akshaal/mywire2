/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package utils
package stupdate

import info.akshaal.jacore.`package`._

/**
 * Update for a boolean state.
 * @param state new state
 * @param validTime how this state is valid
 */
final class BooleanStateUpdate (state : Boolean, validTime : TimeValue)
                extends StateUpdate[Boolean] (state, validTime)
{
    /**
     * Returns thenState update if current state update is true, or elseState otherwise.
     */
    def ifThenElse[T] (thenState : StateUpdate[T], elseState : StateUpdate[T]) : StateUpdate[T] = {
        if (state) thenState else elseState
    }

    /**
     * Perform 'and' operation between current state update and other state update.
     */
    def and (that : BooleanStateUpdate) : BooleanStateUpdate = {
        if (state && that.state) {
            new BooleanStateUpdate (true, validTime.min (that.validTime))
        } else if (!state && !that.state) {
            new BooleanStateUpdate (false, validTime.max (that.validTime))
        } else if (state) {
            that
        } else {
            this
        }
    }

    // Alias
    def && (that : BooleanStateUpdate) = and (that)

    /**
     * Perform 'or' operation between current state update and other state update.
     */
    def or (that : BooleanStateUpdate) : BooleanStateUpdate = {
        if (state && that.state) {
            new BooleanStateUpdate (true, validTime.max (that.validTime))
        } else if (!state && !that.state) {
            new BooleanStateUpdate (false, validTime.min (that.validTime))
        } else if (state) {
            this
        } else {
            that
        }
    }

    // Alias
    def || (that : BooleanStateUpdate) = or (that)

    /**
     * Not operation for boolean state.
     */
    def unary_! (): BooleanStateUpdate = new BooleanStateUpdate (!state, validTime)
}

/**
 * Helper.
 */
object BooleanStateUpdate {
    implicit def boolean2BooleanStateUpdate (b : Boolean) : BooleanStateUpdate =
        new BooleanStateUpdate (state = b, validTime = Long.MaxValue nanoseconds)
}