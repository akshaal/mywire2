/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils

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

    /**
     * Not operation for boolean state.
     */
    def unary_! (): BooleanStateUpdate = new BooleanStateUpdate (!state, validTime)
}
